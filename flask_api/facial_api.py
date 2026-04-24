from flask import Flask, request, jsonify
from flask_cors import CORS
import numpy as np
import base64
import cv2
import os
import tempfile
import pickle
import time
from deepface import DeepFace

app = Flask(__name__)
CORS(app)

# Dossiers pour les photos et encodages
UPLOADS_DIR = "../uploads"
KNOWN_FACES_DIR = "known_faces"
os.makedirs(KNOWN_FACES_DIR, exist_ok=True)

# Seuil de correspondance (plus bas = plus strict)
# 0.4 = très strict, 0.6 = normal, 0.8 = très laxiste
MATCH_THRESHOLD = 0.65  # Augmenté pour être plus tolérant

face_encodings = {}

def get_user_photo_path(user_id):
    """Trouve la photo de l'utilisateur dans le dossier uploads"""
    uploads_path = os.path.join(os.path.dirname(__file__), UPLOADS_DIR)
    uploads_path = os.path.abspath(uploads_path)

    print(f"🔍 Recherche photo pour user {user_id} dans: {uploads_path}")

    best_photo = None
    newest_time = 0

    if os.path.exists(uploads_path):
        for filename in os.listdir(uploads_path):
            full_path = os.path.join(uploads_path, filename)
            if os.path.isfile(full_path) and filename.lower().endswith(('.jpg', '.jpeg', '.png')):
                mod_time = os.path.getmtime(full_path)
                if mod_time > newest_time:
                    newest_time = mod_time
                    best_photo = full_path
                print(f"   📷 Trouvé: {filename}")

    if best_photo:
        print(f"✅ Photo sélectionnée: {best_photo}")
    else:
        print(f"❌ Aucune photo trouvée dans {uploads_path}")

    return best_photo

def load_known_faces():
    global face_encodings
    face_encodings = {}
    for filename in os.listdir(KNOWN_FACES_DIR):
        if filename.endswith(".pickle"):
            user_id = filename.replace(".pickle", "")
            try:
                with open(os.path.join(KNOWN_FACES_DIR, filename), "rb") as f:
                    face_encodings[user_id] = pickle.load(f)
                print(f"✅ Chargé encodage pour user {user_id}")
            except Exception as e:
                print(f"❌ Erreur chargement {user_id}: {e}")

def save_face_encoding(user_id, encoding):
    with open(os.path.join(KNOWN_FACES_DIR, f"{user_id}.pickle"), "wb") as f:
        pickle.dump(encoding, f)
    face_encodings[str(user_id)] = encoding
    print(f"✅ Encodage sauvegardé pour user {user_id}")

def base64_to_image(base64_string):
    """Décoder une image base64 en image OpenCV"""
    if ',' in base64_string:
        base64_string = base64_string.split(',')[1]
    img_bytes = base64.b64decode(base64_string)
    np_arr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    return img

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'message': 'Face recognition server running', 'threshold': MATCH_THRESHOLD})

@app.route('/register-face', methods=['POST'])
def register_face():
    try:
        data = request.get_json()
        user_id = str(data.get('user_id'))
        image_base64 = data.get('image')

        if not image_base64:
            return jsonify({'success': False, 'error': 'No image provided'}), 400

        image = base64_to_image(image_base64)

        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".jpg", mode='wb')
        temp_file.close()

        success, encoded_img = cv2.imencode('.jpg', image)
        if success:
            with open(temp_file.name, 'wb') as f:
                f.write(encoded_img.tobytes())

        time.sleep(0.5)

        try:
            # Utiliser VGG-Face (plus tolérant) au lieu de Facenet
            embedding = DeepFace.represent(img_path=temp_file.name, model_name='VGG-Face', enforce_detection=False)[0]["embedding"]
            save_face_encoding(user_id, embedding)
            try:
                os.unlink(temp_file.name)
            except:
                pass
            return jsonify({'success': True, 'message': 'Face registered successfully'})
        except Exception as e:
            try:
                os.unlink(temp_file.name)
            except:
                pass
            return jsonify({'success': False, 'error': f'Face detection failed: {str(e)}'}), 400

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/verify-face', methods=['POST'])
def verify_face():
    temp_file = None
    try:
        data = request.get_json()
        user_id = str(data.get('user_id'))
        image_base64 = data.get('image')

        if not image_base64:
            return jsonify({'match': False, 'error': 'No image provided'}), 400

        photo_path = get_user_photo_path(user_id)

        if not photo_path or not os.path.exists(photo_path):
            return jsonify({'match': False, 'error': 'User photo not found - please upload a profile photo first'}), 404

        print(f"📸 Photo de profil: {photo_path}")

        webcam_image = base64_to_image(image_base64)

        # Sauvegarder l'image webcam
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".jpg", mode='wb')
        temp_file.close()

        success, encoded_img = cv2.imencode('.jpg', webcam_image)
        if success:
            with open(temp_file.name, 'wb') as f:
                f.write(encoded_img.tobytes())

        time.sleep(0.3)

        print(f"📸 Image webcam temporaire: {temp_file.name}")

        # Comparer les deux visages
        try:
            # Essayer d'abord avec VGG-Face (plus tolérant)
            result = DeepFace.verify(
                img1_path=temp_file.name,
                img2_path=photo_path,
                model_name="VGG-Face",
                enforce_detection=False
            )

            is_match = result["verified"]
            distance = result["distance"]

            # Appliquer notre seuil personnalisé
            is_match = distance < MATCH_THRESHOLD

            confidence = round((1 - min(distance, 1.0)) * 100, 1)

            print(f"📊 Résultat: distance={distance:.4f}, seuil={MATCH_THRESHOLD}, match={is_match}, confidence={confidence}%")

            return jsonify({
                'match': is_match,
                'confidence': confidence,
                'distance': round(distance, 4),
                'threshold': MATCH_THRESHOLD
            })
        except Exception as e:
            return jsonify({'match': False, 'error': f'Face comparison failed: {str(e)}'}), 400
        finally:
            try:
                if temp_file and os.path.exists(temp_file.name):
                    os.unlink(temp_file.name)
            except:
                pass

    except Exception as e:
        return jsonify({'match': False, 'error': str(e)}), 500

@app.route('/update-user-photo', methods=['POST'])
def update_user_photo():
    try:
        data = request.get_json()
        user_id = str(data.get('user_id'))
        photo_path = data.get('photo_path')

        if not photo_path:
            return jsonify({'success': False, 'error': 'No photo path provided'}), 400

        if not os.path.exists(photo_path):
            return jsonify({'success': False, 'error': 'Photo file not found'}), 404

        try:
            embedding = DeepFace.represent(img_path=photo_path, model_name='VGG-Face', enforce_detection=False)[0]["embedding"]
            save_face_encoding(user_id, embedding)
            return jsonify({'success': True, 'message': 'User photo updated successfully'})
        except Exception as e:
            return jsonify({'success': False, 'error': f'Face detection failed: {str(e)}'}), 400

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/set-threshold', methods=['POST'])
def set_threshold():
    global MATCH_THRESHOLD
    data = request.get_json()
    new_threshold = data.get('threshold', 0.65)
    MATCH_THRESHOLD = new_threshold
    return jsonify({'success': True, 'threshold': MATCH_THRESHOLD})

if __name__ == '__main__':
    load_known_faces()
    print("\n" + "="*50)
    print("🚀 Serveur de reconnaissance faciale démarré")
    print("="*50)
    print(f"📍 URL: http://localhost:5001")
    print(f"🎯 Seuil de correspondance: {MATCH_THRESHOLD}")
    print(f"📁 Dossier encodages: {KNOWN_FACES_DIR}")
    print(f"📁 Dossier photos uploads: {UPLOADS_DIR}")
    print("="*50 + "\n")
    app.run(host='0.0.0.0', port=5001, debug=False)