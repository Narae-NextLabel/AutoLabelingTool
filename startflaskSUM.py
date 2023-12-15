from flask import Flask, request, jsonify
import pandas as pd
from flask_cors import CORS
import pickle
import os 

import yaml
import numpy as np
from super_gradients.training import models
from super_gradients.common.object_names import Models
from ultralytics import YOLO
import torch
from PIL import Image
import cv2
from collections import OrderedDict
import shutil
import random

print("PyTorch version:", torch.__version__)
print("CUDA is available:", torch.cuda.is_available())
print(os.getcwd())
device = 'cuda' #if torch.cuda.is_available() else "cpu"
app = Flask(__name__)
CORS(app)
global current_directory
current_directory = os.getcwd()
current_directory = current_directory.replace("\\", "/")
#####################################################################
def yolo_to_coco(x_center, y_center, w, h,  image_w, image_h):
    w = w * image_w
    h = h * image_h
    x1 = ((2 * x_center * image_w) - w)/2
    y1 = ((2 * y_center * image_h) - h)/2
    return [x1, y1, w, h]

def coco_to_yolo(x1, y1, w, h, image_w, image_h):
    return [((2*x1 + w)/(2*image_w)) , ((2*y1 + h)/(2*image_h)), w/image_w, h/image_h]

def convert_xywh_to_xyxy(xywh):
    center_x, center_y, width, height = xywh

    x1 = center_x - (width / 2)
    y1 = center_y - (height / 2)
    x2 = center_x + (width / 2)
    y2 = center_y + (height / 2)
    return [x1, y1, x2, y2]

def find_max_folder_number(directory, keyword):
    subfolders = [f.path for f in os.scandir(directory) if f.is_dir()]
    max_folder_number = -1  # 초기값 설정

    for folder in subfolders:
        if keyword in folder:
            parts = folder.split(keyword)
            if len(parts) == 2 and parts[1].isdigit():
                folder_number = int(parts[1])
                if folder_number > max_folder_number:
                    max_folder_number = folder_number

    if max_folder_number == -1:
        return keyword  # 숫자가 없는 경우
    else:
        return f'{keyword}{max_folder_number}'  # 숫자가 있는 경우
########################################################################

##################학습 전처리####################################
@app.route('/checkClassNames', methods=['POST'])
def check_classnames():
    global current_directory
    global classnames
    data = request.get_json()
    image_path = "/src/main/resources/static/"
    idx = data.get('idx')
    user_name = data.get('username')
    to_idx_dir = f'{current_directory}{image_path}{user_name}/{idx}'
    print(to_idx_dir)
    last_result_folder_name = find_max_folder_number(to_idx_dir, 'result')  # idx하위에 있는 것 중 가장 큰 resultN폴더
    app.logger.info("yaml 저장실행되냐?")
    classnames = data.get('classnames')
    classnames = list(OrderedDict.fromkeys(classnames))
    yaml_data = {'train': 'train경로',
                 'val': 'val경로',
                 'test': 'test경로',
                 'names': classnames,
                 'nc': len(classnames)
                 }
    app.logger.info(classnames)
    # result_folder_name에 있는 숫자를 가져와서 1을 더함
    current_number = int(last_result_folder_name.split('result')[-1])
    new_number = current_number + 1
    # 다음 result
    result_folder_name = f'result{new_number}'
    # 전result
    before_folder_name = f'result{current_number}'
    # 새로운 resultN폴더 생성
    new_folder_path = os.path.join(to_idx_dir, result_folder_name)
    os.makedirs(new_folder_path)
    with open(f'{current_directory}{image_path}{user_name}/{idx}/{result_folder_name}/data.yaml', 'w') as f:
        yaml.dump(yaml_data, f)
    with open(f'{current_directory}{image_path}{user_name}/{idx}/{before_folder_name}/data.yaml', 'w') as f:
        yaml.dump(yaml_data, f)
    with open(f'{current_directory}{image_path}{user_name}/{idx}/{before_folder_name}/data.yaml', 'r') as f:
        data_yaml = yaml.safe_load(f)
    classnames=data_yaml['names']
    app.logger.info(classnames)
    # Check the label of the snowflake
    label_folder_path = os.path.join(f'{current_directory}{image_path}{user_name}/{idx}/{before_folder_name}/labels')
    for filename in os.listdir(label_folder_path):
        app.logger.info('읽기!!')
        file_path = os.path.join(label_folder_path, filename)
        with open(file_path, 'r') as label_file:  # Label_file: 'file' -> 'label_file'
            lines = label_file.readlines()
            app.logger.info(label_file)
        classname = []
        bbox = []
        for line in lines:
            result = line.strip().split()
            if len(result) == 5:
                app.logger.info('한글자')
                classname.append(result[0])
                x, y, w, h = [float(coord) for coord in result[1:]]
                bbox.append((x, y, w, h))
            else:
                app.logger.info('두글자')
                combined_classname = f"{result[0]} {result[1]}"
                classname.append(combined_classname)
                x, y, w, h = [float(coord) for coord in result[2:]]
                bbox.append((x, y, w, h))
        classnames_index_list = [classnames.index(cls) for cls in classname]
        with open(file_path, 'w') as label_file:
            for cls_index, bb in zip(classnames_index_list, bbox):
                x, y, w, h = bb
                label_file.write(f"{cls_index} {x} {y} {w} {h}\n")
    return jsonify("success")

@app.route('/whatIsUsingModel', methods=['POST'])
def getUsingModel():
    global using_model
    data = request.get_json()  # JSON 데이터 받기
    using_model = data.get('usingModel')  # 'usingModel' 키의 값을 가져와 전역변수에 저장

    if not data or 'usingModel' not in data or not data['usingModel']:
        return jsonify({"error": "No data received or usingModel is empty"}), 400  # 클라이언트에게 오류 응답

    print("받은 usingModel 값:", using_model)  # 서버 측 콘솔에 출력

    return jsonify({"message": "Received"}), 200  # 클라이언트에게 응답 보내기



        #################################train/test파일경로 생성########################
@app.route('/startTraining', methods=['POST'])
def start_training():
    data = request.get_json()
    user_name = data.get('username')
    global current_directory
    
    image_path = "/src/main/resources/static/"
    idx = data.get('idx')
    to_idx_dir=f'{current_directory}/{image_path}/{user_name}/{idx}'
    result_folder_name = find_max_folder_number(to_idx_dir, 'result') # idx하위에 있는 것 중 가장 큰 resultN폴더
    trainClassNames=[] 
    app.logger.info(result_folder_name)
    yaml_dir=f'{current_directory}{image_path}{user_name}/{idx}/{result_folder_name}/data.yaml'
    def update_yaml(yaml_dir, train_images_folder, test_images_folder):
    # YAML 파일 읽기
        nonlocal trainClassNames  # 상위 스코프의 trainClassNames 변수를 사용
        with open(yaml_dir, 'r') as file:
            data_yaml = yaml.safe_load(file)
        # 경로 업데이트
        data_yaml['train'] = train_images_folder
        data_yaml['val'] = train_images_folder
        data_yaml['test'] = test_images_folder

        # YAML 파일 쓰기
        with open(yaml_dir, 'w') as file:
            yaml.dump(data_yaml, file, default_flow_style=False)

    def split_data(src_folder, dest_folder, train_ratio=0.8):
        files = [f for f in os.listdir(src_folder) if os.path.isfile(os.path.join(src_folder, f))]
        random.shuffle(files)

        split_index = int(len(files) * train_ratio)

        train_files = set(files[:split_index])  # 집합으로 변환
        test_files = set(files[split_index:])   # 집합으로 변환

        # 중복 제거
        test_files = test_files - train_files

        train_images_folder = os.path.join(dest_folder, 'train', 'images').replace('\\', '/')
        test_images_folder = os.path.join(dest_folder, 'test', 'images').replace('\\', '/')
        os.makedirs(train_images_folder, exist_ok=True)
        os.makedirs(test_images_folder, exist_ok=True)
        for file in train_files:
            shutil.copy(os.path.join(src_folder, file), os.path.join(train_images_folder, file))

        for file in test_files:
            shutil.copy(os.path.join(src_folder, file), os.path.join(test_images_folder, file))
        # yaml파일 img참조 경로 수정
        update_yaml(yaml_dir, train_images_folder, test_images_folder)

        return train_files, test_files

    # 이전 label의 yaml class인덱스 값을 현재 yaml labelindex에 맞게 수정
    def update_label_indices(src_label_file, yaml_dir_before, yaml_dir_current):#복사대상폴더경로+파일명, 이전 yaml경로, 현재 yaml 경로
        with open(yaml_dir_before, 'r') as file:
            data_yaml_before = yaml.safe_load(file)
        with open(yaml_dir_current, 'r') as file:
            data_yaml_current = yaml.safe_load(file)

        updated_label_data = ""
        with open(src_label_file, 'r') as file:
            for line in file:
                if line.strip():
                    parts = line.strip().split(' ')
                    class_index_before = int(parts[0])
                    class_name = data_yaml_before['names'][class_index_before]
                    if class_name in data_yaml_current['names']:
                        new_class_index = data_yaml_current['names'].index(class_name)
                        parts[0] = str(new_class_index)
                    updated_label_data += ' '.join(parts) + '\n'
        return updated_label_data

    def copy_labels(src_folder, dest_folder, file_list, yaml_dir_before, yaml_dir_current, ext='.txt'):
        os.makedirs(dest_folder, exist_ok=True)
        for file in file_list:
            base_filename = os.path.splitext(file)[0]
            label_file = base_filename + ext
            src_label_file = os.path.join(src_folder, label_file)
            dest_label_file = os.path.join(dest_folder, label_file)
            if os.path.exists(src_label_file):
                updated_label_data = update_label_indices(src_label_file, yaml_dir_before, yaml_dir_current)
                with open(dest_label_file, 'w') as file:
                    file.write(updated_label_data)

    def copy_labels_to_current_result_folder(source_folder, dest_folder):
        if not os.path.exists(dest_folder):
            os.makedirs(dest_folder, exist_ok=True)
        
        for label_file in os.listdir(source_folder):
            full_file_path = os.path.join(source_folder, label_file)
            if os.path.isfile(full_file_path):
                shutil.copy(full_file_path, dest_folder)

    # 데이터 분할 및 복사를 위한 코드
    image_src_folder = f'{current_directory}{image_path}{user_name}/{idx}/image'
    last_result_folder_name = find_max_folder_number(to_idx_dir, 'result')
    current_number = int(last_result_folder_name.split('result')[-1])
    new_number = current_number - 1
        # 새로운 폴더 이름 생성
    result_folder_name_before = f'result{new_number}'
    app.logger.info(result_folder_name)
    app.logger.info(result_folder_name_before)
    label_src_folder = f'{current_directory}{image_path}{user_name}/{idx}/{result_folder_name_before}/labels'
    dest_folder = f'{current_directory}{image_path}{user_name}/{idx}/{result_folder_name}'

    # yaml 파일 경로 설정
    yaml_dir_before = f'{current_directory}{image_path}{user_name}/{idx}/{result_folder_name_before}/data.yaml'
    yaml_dir_current = f'{current_directory}{image_path}{user_name}/{idx}/{result_folder_name}/data.yaml'

    # 이미지 및 레이블 데이터 분할
    train_files, test_files = split_data(image_src_folder, dest_folder)

    # 레이블 복사
    train_labels_folder = os.path.join(dest_folder, 'train', 'labels')
    test_labels_folder = os.path.join(dest_folder, 'test', 'labels')
    copy_labels(label_src_folder, train_labels_folder, train_files, yaml_dir_before, yaml_dir_current)
    copy_labels(label_src_folder, test_labels_folder, test_files, yaml_dir_before, yaml_dir_current)

    # 새로운 labels 폴더 생성 및 복사
    dest_labels_folder = os.path.join(dest_folder, 'labels')
    os.makedirs(dest_labels_folder, exist_ok=True)

    # train 및 test 레이블 파일들을 dest_folder의 labels에 복사

    copy_labels_to_current_result_folder(train_labels_folder, dest_labels_folder)
    copy_labels_to_current_result_folder(test_labels_folder, dest_labels_folder)

    # dest_folder를 현재 작업 디렉토리로 설정
    os.chdir(dest_folder)

    app.logger.info(trainClassNames)
    model = YOLO(f'{current_directory}/src/main/resources/static/assets/models/yolov8x.pt')
    
    
    DEVICE = 'cuda' if torch.cuda.is_available() else "cpu"
    app.logger.info(DEVICE)
    
    model.train(data=yaml_dir, epochs=5, patience=5, batch=4, imgsz=640,  device=DEVICE)
    best_pt_dir=f'{current_directory}/src/main/resources/static/{user_name}/{idx}/{result_folder_name}/runs/detect/weights/best.pt'#dest_folder+'/runs/detect/train/weights/best.pt'
    # app.logger.info(trainClassNames)
    # best_model=YOLO(best_pt_dir)
    # best_preidction=best_model.predict(f'{image_src_folder}', save_txt=True, imgsz=320, conf=0.1)
    # app.logger.info(best_preidction)
    

    return jsonify({'bestPtDir':f'{best_pt_dir}', 'projectIdx':f'{idx}', 'trainClassNames':f'{trainClassNames}'})

#####학습 후 predict

@app.route('/predictAfterTrain', methods=['POST'])
def predict_afterTrain():
    global current_directory
    data = request.get_json()
    user_name = data.get('userName')
    projectIdx=data.get('projectIdx')
    lastModelIdx=data.get('lastModelIdx')
    addModelIdx=int(lastModelIdx)+1
    result_folder_name = find_max_folder_number(f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}','result')
    train_folder_name=find_max_folder_number(f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/runs/detect', 'train')
    best_pt_dir=f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/runs/detect/{train_folder_name}/weights/best.pt'#dest_folder+'/runs/detect/train/weights/best.pt'
    image_src_folder=f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/image'
    yaml_dir=f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/data.yaml'
    trainClassNames=[]
     # 상위 스코프의 trainClassNames 변수를 사용
    with open(yaml_dir, 'r') as file:
        data_yaml = yaml.safe_load(file)
        if 'names' in data_yaml:
            trainClassNames = data_yaml['names']
    best_model=YOLO(best_pt_dir)
    new_path=f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}'
    new_path=os.chdir(new_path)
    app.logger.info(new_path)
    #results=best_model(f'C:/Users/smhrd/label/src/main/resources/static/test1/{projectIdx}/image', save_txt=True)
    #for result in results:
    #    app.logger.info(result)
    best_model.predict(f'{image_src_folder}', save_txt=True, imgsz=640, conf=0.7)
    predict_folder_name=find_max_folder_number(f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/runs/detect', 'predict')
    predict_dir=f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/runs/detect/{predict_folder_name}/labels/'
    file_names = [file[:-4] for file in os.listdir(predict_dir) if file.endswith('.txt')]

    all_image_files = os.listdir(image_src_folder)
    model_name=f'{user_name}{addModelIdx}'
    model_file_name=f'{user_name}{addModelIdx}.pt'
    copy_current_directory = os.path.normpath(current_directory)
    #best.pt 복사해서 models에 옮기기(이름도 변경)
    # 원본 파일 경로
    source_file = os.path.join(copy_current_directory, 'src', 'main', 'resources', 'static', user_name, projectIdx, result_folder_name, 'runs', 'detect', train_folder_name, 'weights', 'best.pt')
    # 대상 디렉토리 경로
    destination_dir = os.path.join(copy_current_directory, 'src', 'main', 'resources', 'static', 'assets', 'models')
    # 복사할 파일의 새로운 경로 (파일 이름 포함)
    destination_file = os.path.join(destination_dir, model_file_name)
    
    # 파일 복사
    shutil.copy(source_file, destination_file)
    print(model_file_name)
    print(destination_dir)
    befor_labels = os.listdir(f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/labels')
    for label in befor_labels:
        label_path = os.path.join(f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/labels', label)
        os.remove(label_path)

    # 기존 폴더 삭제
    shutil.rmtree(f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/labels')

    # shutil.copy 대신 shutil.copytree 사용
    shutil.copytree(predict_dir, f'{current_directory}/src/main/resources/static/{user_name}/{projectIdx}/{result_folder_name}/labels')
    # 이미지 파일 리스트 초기화
    image_files = []
    # static/ 이후 경로만 담아주기
    image_files = [os.path.join(image_src_folder, file).replace('\\', '/').split('static/')[1] for file in all_image_files]
   
    return jsonify({'trainClassNames': trainClassNames, 'fileNames':file_names, 'imagePathList':image_files, 'modelName':model_name, 'resultFolderName':result_folder_name})
#################box,classname TXT로 저장###################################
@app.route('/savetxt', methods=['POST'])
def save_txt():
    data = request.get_json()
    global current_directory
    basic_path = "/src/main/resources/static/"
    user_name=data.get('username')
    idx=data.get('idx')
    label_folder=data.get('labelFolder')
    parent_directory = os.path.dirname(label_folder)
    result_folder_name = os.path.basename(parent_directory)
    app.logger.info(label_folder)
    app.logger.info(result_folder_name)
    filename = data.get('filename')
    height = float(data.get('height'))
    width = float(data.get('width'))
    classname = data.get('classname')
    bbox = data.get('bbox')
    filename=os.path.splitext(filename)[0]
    output_file_path = os.path.join(f'{current_directory}{basic_path}/{user_name}/{idx}/{result_folder_name}/labels/{filename}.txt')
    with open(output_file_path, 'w') as file:
        for cls, bb in zip(classname, bbox):
            x,y,w,h=coco_to_yolo(bb[0],bb[1],bb[2],bb[3],width,height)
            file.write(f"{cls} {x} {y} {w} {h}\n")
    return jsonify({'message': 'Data processed successfully'})
####################라벨 가져오기############################################
@app.route('/loadtxt', methods={'POST'})
def load_txt():
    data = request.get_json()
    global current_directory
    basic_path = "/src/main/resources/static/"
    user_name=data.get('username')
    idx=data.get('idx')
    label_folder=data.get('labelFolder')
    parent_directory = os.path.dirname(label_folder)
    result_folder_name = os.path.basename(parent_directory)
    app.logger.info(label_folder)
    app.logger.info(result_folder_name)
    with open(f'{current_directory}{basic_path}/{user_name}/{idx}/{result_folder_name}/data.yaml','r') as f:
        data_yaml=yaml.safe_load(f)
    classnames = data_yaml['names']
    classname=[]
    bbox=[]
    filename = data.get('filename')
    height = float(data.get('height'))
    width = float(data.get('width'))
    filename=os.path.splitext(filename)[0]
    output_file_path = os.path.join(f'{current_directory}{basic_path}{user_name}/{idx}/{result_folder_name}/labels/{filename}.txt')
    if not os.path.exists(output_file_path):
        # 파일이 존재하지 않으면 새로 생성
        os.makedirs(os.path.dirname(output_file_path), exist_ok=True)
        with open(output_file_path, 'w') as new_file:
            # 원하는 내용으로 파일 초기화
            new_file.write("")
    with open(output_file_path, 'r') as file:
        lines = file.readlines()
        for line in lines:
            result =line.strip().split()
            if len(result) == 5:
                if result[0].isdigit():  # 문자열이 숫자로 이루어져 있는지 확인
                    index = int(result[0])
                    classname.append(classnames[index])
                else:
                    classname.append(result[0])
                x, y, w, h = [float(coord) for coord in result[1:]]
                xyxy = yolo_to_coco(x,y,w,h, width, height)
                bbox.append(xyxy)
            else:
                combined_classname = f"{result[0]} {result[1]}"
                if combined_classname.isdigit():
                    index = int(combined_classname)
                    classname.append(classnames[index])
                else:
                    classname.append(combined_classname)
                x, y, w, h = [float(coord) for coord in result[2:]]
                xyxy = yolo_to_coco(x,y,w,h, width, height)
                bbox.append(xyxy)
    app.logger.info(classname)
    data_send = {'classname':classname,'bbox':bbox}
    return jsonify(data_send)

##################################################### 예측#####################################
class_names=[]
@app.route('/predict', methods=['POST'])
def predict():
    global prediction_completed
    try:
        data= request.get_json()  # 이미지 경로 수신
        image_path =data.get('imagePath') # /src/main/resources/static/
        user_name=data.get('userName') # 사용자 이름
        project_idx=data.get('projectIdx') # 프로젝트 폴더
        global current_directory
        real_path=f"{current_directory}{image_path}{user_name}/{project_idx}/image/" # 다합친 사용자에 따른 이미지들 경로
        # 지정된 경로
        project_path = f"{current_directory}{image_path}{user_name}/{project_idx}"
        label_file_path=f"{current_directory}{image_path}{user_name}/{project_idx}/labels"

        # 해당 경로에 있는 파일 및 폴더의 개수 확인
        cnt = len([name for name in os.listdir(project_path) if os.path.isdir(os.path.join(project_path, name)) and 'result' in name])+1
        
        # 새로운 폴더 이름 설정 (예: result1, result2, ...)
        result_folder_name = f"result{cnt}"

        # 새로운 폴더 경로 생성
        result_folder_path = os.path.join(project_path, result_folder_name)

        # 새로운 폴더 생성
        if not os.path.exists(result_folder_path):
            os.makedirs(result_folder_path)
        
        label_dir = f"{result_folder_path}/labels/" # 저장될 txt 경로
        if not os.path.exists(label_dir):
            os.makedirs(label_dir)
        file_names = os.listdir(real_path)
        global using_model
        print("첫 predict 직전 사용할 모델:",using_model)
        #################################@@@@@@@@@########### YOLO NAS 사용하는 경우####@@@@@@@@@@@@@@@@@@@#################################
        if using_model == 'Standard80':
            
            nas_model = models.get(Models.YOLO_NAS_L, pretrained_weights="coco")
            
            print("listdir을 통해 나온 이미지 file 이름들 : ", file_names)
            # app.logger.info(file_names)
            _, extension = os.path.splitext(file_names[0]) # 동영상인지 걸러내는 
            video_extensions = ['.mp4', '.avi', '.mkv'] 
            image_extensions = ['.jpg', '.jpeg', '.png']
            #app.logger.info(extension)
            is_video = extension.lower() in video_extensions
            
            # 업로드한 파일이 비디오일경우 기능
            if is_video :
                video_path = f"{real_path}{file_names[0]}"
                video_name, video_extension = os.path.splitext(file_names[0])
                app.logger.info("파일이름 : %s",video_name)
                app.logger.info("파일확장자 : %s",video_extension)
                
                #################video 폴더 없으면 생성####
                if not os.path.exists(f"{current_directory}{image_path}{user_name}/{project_idx}/video/"):
                    os.makedirs(f"{current_directory}{image_path}{user_name}/{project_idx}/video/")
                        
                # 비디오 캡처 객체 생성
                video = cv2.VideoCapture(video_path)
                desired_fps = 3
                frame_interval = int(video.get(cv2.CAP_PROP_FPS) / desired_fps)
                success_list = []  # 각 프레임의 저장 여부를 저장할 리스트        
                ###predict영상 저장####
                save_video_path = f'{current_directory}{image_path}{user_name}/{project_idx}/video/{video_name}_predict.mp4'
                nas_model.to(device).predict(video_path).save(save_video_path)
            
                ###########사용자 지정 프레임 이미지 저장#############
                for i in range(int(video.get(cv2.CAP_PROP_FRAME_COUNT))):
                    ret, frame = video.read()
                    if ret and i % frame_interval == 0:  # frame_interval 간격으로만 프레임을 선택
                        save_path = real_path
                        fake_path = f"{current_directory}{image_path}fake/{video_name}_{i}.jpg"
                        success = cv2.imwrite(fake_path, frame)
                        if success:
                            shutil.move(fake_path, save_path)
                            success_list.append(f'{video_name}_{i}.jpg')
                            app.logger.info(f"이미지 저장 성공: {save_path}")
                        else:
                            app.logger.info(f"이미지 저장 실패: {save_path}")
                video.release()  # 비디오 캡처 객체 해제
                #############################################################
                shutil.move(video_path, f"{current_directory}{image_path}{user_name}/{project_idx}/video/")
                
                # 이미지 파일만 추려서 예측 수행
                file_names = os.listdir(real_path)
                image_list = [os.path.join(real_path, file) for file in file_names if os.path.splitext(file)[1].lower() in image_extensions]
                images_predictions = nas_model.to(device).predict(image_list) 
                processed_images = 0
                for image_prediction in images_predictions:
                    image_prediction.image
                    class_names = image_prediction.class_names
                    labels = image_prediction.prediction.labels
                    confidence = image_prediction.prediction.confidence
                    bboxes = image_prediction.prediction.bboxes_xyxy
                def get_image_size(real_path):
                    with Image.open(real_path) as img:
                        return img.width, img.height
                app.logger.info(f"클래스이름: {class_names}")
                def save_detection_results(image_name, labels, bboxes, real_path):
                    image_width, image_height = get_image_size(real_path)
                    app.logger.info(image_name)
                    with open(f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/labels/{image_name}.txt", 'w') as file:
                        for label, bbox in zip(labels, bboxes):
                            x_center = (bbox[0] + bbox[2]) / 2 / image_width
                            y_center = (bbox[1] + bbox[3]) / 2 / image_height
                            width = (bbox[2] - bbox[0]) / image_width
                            height = (bbox[3] - bbox[1]) / image_height
                            file.write(f"{int(label)} {x_center} {y_center} {width} {height}\n")
                for file_path, image_prediction in zip(image_list, images_predictions):
                            # 이미지 파일의 이름만 추출 (확장자 제외)
                            image_name = os.path.splitext(os.path.basename(file_path))[0]
                            # 추출된 이미지 이름으로 결과 저장
                            # app.logger.info(image_name)
                            save_detection_results(image_name, image_prediction.prediction.labels, image_prediction.prediction.bboxes_xyxy, file_path)
                            
                            processed_images += 1  # 처리된 이미지 수 업데이트
                if class_names is not None:
                    class_names = list(OrderedDict.fromkeys(class_names))
                    yaml_data = {'train':'train경로',
                                'val':'val경로',
                                'test':'test경로',
                                'names' : class_names,
                                'nc':len(class_names)
                    }
                    with open(f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/data.yaml",'w') as f:
                        yaml.dump(yaml_data,f)    

                label_path=f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/labels/"+image_name+'.txt'
                label_path = label_path.replace("\\", "/")
                ### 비디오 웹에서 띄워주기위해 인코딩####
                Vid = cv2.VideoCapture(save_video_path)
                if Vid.isOpened():
                    fps = Vid.get(cv2.CAP_PROP_FPS)
                    f_width = round(Vid.get(cv2.CAP_PROP_FRAME_WIDTH))
                    f_height = round(Vid.get(cv2.CAP_PROP_FRAME_HEIGHT))
                    codec = cv2.VideoWriter_fourcc(*'VP80')
                    encoded_avi = cv2.VideoWriter(f'{current_directory}{image_path}{user_name}/{project_idx}/video/{video_name}_predict.webm', codec, fps, (f_width, f_height))
                    print("Is it running?")
                else:
                    print("Cannot open video file.")
                frame_count = 0  # 추가된 부분
                while Vid.isOpened():
                    ret, frame = Vid.read()
                    print(f"Is it running? 2222 - Frame {frame_count}") 
                    if ret:
                        key = cv2.waitKey(1)
                        try:
                            encoded_avi.write(frame)
                        except Exception as e:
                            print(f"Error writing frame {frame_count}: {e}")  # 추가된 부분
                        if key == ord('q'):
                            break
                    else:
                        print(f"Error reading frame {frame_count}.")
                        break
                    frame_count += 1  # 추가된 부분
                Vid.release()
                encoded_avi.release()

            
                ################################################################
                return jsonify({"classNames": class_names, "imageNames":image_list, "labelPath":label_path,"resultFolderName":result_folder_name, "video":True,"fileName":success_list,"projectIdx":project_idx})
            else : 
                #app.logger.info(file_names)
                image_list=[]
                
                processed_images = 0

                for file_name in file_names:
                    source = os.path.join(real_path, file_name)
                    image_list.append(source)
                    print('이미지 경로',source)
                    # YOLO object detection
                images_predictions = nas_model.to(device).predict(image_list)

                for image_prediction in images_predictions:
                    image_prediction.image
                    class_names = image_prediction.class_names
                    labels = image_prediction.prediction.labels
                    confidence = image_prediction.prediction.confidence
                    bboxes = image_prediction.prediction.bboxes_xyxy

                # save option # 예측 결과 처리 및 저장(디텍션 박스 그려진 이미지 저장)
                #images_predictions.save(output_folder='./label', box_thickness=2, show_confidence=True)
                #같은 위치에 같은 이름이면 새로 실행해도 덮어씌워짐
                
                def get_image_size(real_path):
                    with Image.open(real_path) as img:
                        return img.width, img.height
                
                def save_detection_results(image_name, labels, bboxes, real_path):
                    image_width, image_height = get_image_size(real_path)
                    app.logger.info(image_name)
                    with open(f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/labels/{image_name}.txt", 'w') as file:
                        for label, bbox in zip(labels, bboxes):
                            x_center = (bbox[0] + bbox[2]) / 2 / image_width
                            y_center = (bbox[1] + bbox[3]) / 2 / image_height
                            width = (bbox[2] - bbox[0]) / image_width
                            height = (bbox[3] - bbox[1]) / image_height
                            file.write(f"{int(label)} {x_center} {y_center} {width} {height}\n")
                

                    # 이미지 감지 결과 처리 및 저장
                for file_path, image_prediction in zip(image_list, images_predictions):
                    # 이미지 파일의 이름만 추출 (확장자 제외)
                    image_name = os.path.splitext(os.path.basename(file_path))[0]
                    # 추출된 이미지 이름으로 결과 저장
                    # app.logger.info(image_name)
                    save_detection_results(image_name, image_prediction.prediction.labels, image_prediction.prediction.bboxes_xyxy, file_path)
                    
                    processed_images += 1  # 처리된 이미지 수 업데이트
                    print(image_list)
                    print('='*15)
                    print('진행된 이미지 수',processed_images)
                    print('='*15)
                if class_names is not None:

                    class_names = list(OrderedDict.fromkeys(class_names))
                    yaml_data = {'train':'train경로',
                                'val':'val경로',
                                'test':'test경로',
                                'names' : class_names,
                                'nc':len(class_names)
                    }
                    with open(f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/data.yaml",'w') as f:
                        yaml.dump(yaml_data,f)
                # opencv
            
            
                label_path=f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/labels/"+image_name+'.txt'
                label_path = label_path.replace("\\", "/")
                prediction_completed=True    
                return jsonify({"classNames": class_names, "imageNames":image_list, "resultFolderName":result_folder_name, "video":False})
            
        else:##################@@@@@@@@@@## (혹은 elseif) => YOLOv8_custom_model인 경우 ##################@$#@#$#$@@@@@@@@##################################
            image_list=[]
            image_name_list=[]
            label_file_path=f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/labels"
            
            for file_name in file_names:
                    source = os.path.join(real_path, file_name)
                    image_list.append(source)
                    print('이미지 경로',source)
                    name_without_extension = os.path.splitext(file_name)[0]
                    image_name_list.append(name_without_extension)

            custom_model_dir=f'{current_directory}{image_path}assets/models/{using_model}.pt'
            app.logger.info(custom_model_dir)
            model=YOLO(custom_model_dir)
            results = model(data=image_list, conf=0.001)  # Results 객체의 리스트 반환
            get_class=model.names
            class_names = list(get_class.values())
            app.logger.info(class_names)
            print(" 커스텀 모델의 class_names:",class_names)
            for idx, result in enumerate(results):
                boxes = result.boxes
                app.logger.info(boxes)
                boxes_array = boxes.xywhn.cpu().numpy()
                class_array = boxes.cls.cpu().numpy()
                print('=======boxes_array')
                print(boxes_array)
                app.logger.info(boxes_array)
                print('===============')
                print('=======class_array')
                print(class_array)
                app.logger.info(class_array)
                print('===============')

                class_string = str(int(class_array[0])) if class_array.size > 0 else "" # 클래스 배열이 비어있지 않은 경우에만 변환
                boxes_string = ' '.join(map(str, boxes_array[0])) if boxes_array.size > 0 else "" # boxes 배열이 비어있지 않은 경우에만 변환
                combined_string = class_string + ' ' + boxes_string if class_string and boxes_string else ""

                if combined_string: # combined_string이 빈 값이 아닐 경우에만 파일에 쓰기
                    label_file = os.path.join(label_file_path, image_name_list[idx] + ".txt").replace('\\','/')
                    with open(label_file, 'w') as file:
                        file.write(combined_string)
                    print("커스텀 모델 사용 첫 predict ___ labels 파일에 저장된 내용:", combined_string)
                else:
                    print("커스텀 모델 사용 첫 predict___저장할 label 내용이 없습니다.")
            if class_names is not None:

                    class_names = list(OrderedDict.fromkeys(class_names))
                    yaml_data = {'train':'train경로',
                                'val':'val경로',
                                'test':'test경로',
                                'names' : class_names,
                                'nc':len(class_names)
                    }
                    with open(f"{current_directory}{image_path}{user_name}/{project_idx}/{result_folder_name}/data.yaml",'w') as f:
                        yaml.dump(yaml_data,f)
            return jsonify({"classNames": class_names, "imageNames":image_list, "resultFolderName":result_folder_name, "video":False})
    except Exception as e:
        print(f"오류 발생: {e}")
        return f"서버 오류: {e}", 50



#predict 이후 이미지 그려주기 전 필요 데이터 전송해주기
@app.route('/getLabelData',methods=['POST'])
def get_label_data():
    # 이미지 파일 이름에서 확장자 제거
    data = request.get_json()
    project_idx = data.get('projectIdx')
    user_name = data.get('userName')
    path = data.get('path')
    image_name = data.get('imageName')
    global current_directory
    result_folder_name=find_max_folder_number(f'{current_directory}{path}{user_name}/{project_idx}', 'result')
    #current_directory = os.getcwd()
    base_name = os.path.splitext(image_name)[0]
    
    
        # 역슬래시를 슬래시로 변경
    #current_directory = current_directory.replace("\\", "/")
    label_file_path = os.path.join(f"{current_directory}{path}{user_name}/{project_idx}/{result_folder_name}/labels/"+ base_name + '.txt')
    app.logger.info(label_file_path)
    try:
        if os.path.exists(label_file_path):
            # 파일 열기 및 내용 읽기
            with open(label_file_path, 'r') as file:
                label_data = file.read()
            return jsonify({"labelData":label_data}) 
        else:
            return "Label file not found.", 404
    except Exception as e:
        return f"An error occurred: {e}", 500

@app.route('/getLabelDataAfterTrain',methods=['POST'])
def get_label_data_afterTrain():
    # 이미지 파일 이름에서 확장자 제거
    data = request.get_json()
    project_idx = data.get('projectIdx')
    user_name = data.get('userName')
    path = data.get('path')
    image_name = data.get('imageName')
    global current_directory
    #current_directory = os.getcwd()
    base_name = os.path.splitext(image_name)[0]
    result_folder_name=find_max_folder_number(f'{current_directory}{path}{user_name}/{project_idx}', 'result')
    current_number = int(result_folder_name.split('result')[-1])
    new_number = current_number - 1
        # 새로운 폴더 이름 생성
    result_folder_name_before = f'result{new_number}'
    predict_folder_name=find_max_folder_number(f'{current_directory}{path}{user_name}/{project_idx}/{result_folder_name}/runs/detect','predict')
    app.logger.info(base_name)
    
    
        # 역슬래시를 슬래시로 변경
    #current_directory = current_directory.replace("\\", "/")
    label_file_path = os.path.join(f"{current_directory}{path}{user_name}/{project_idx}/{result_folder_name}/runs/detect/{predict_folder_name}/labels/"+ base_name + '.txt')
    alt_label_path=os.path.join(f"{current_directory}{path}{user_name}/{project_idx}/{result_folder_name_before}/labels/"+ base_name + '.txt')
    app.logger.info(label_file_path)
    
    try:
        label_data = ""
        if os.path.exists(label_file_path):
            with open(label_file_path, 'r') as file:
                label_data = file.read()

            # runs/detect/predict하위 labels 폴더에 txt가 없다면 이전 resultfolder의 같은 이름의 label 파일을 참조하되,
            # 최근 것의 yaml파일에 맞는 레이블 인덱스 값을 넘겨주어야 함.

        elif os.path.exists(alt_label_path):
            with open(alt_label_path, 'r') as file:
                label_data = file.read()
            
            # data.yaml 파일들의 경로
            data_yaml_path_before = os.path.join(f"{current_directory}{path}{user_name}/{project_idx}/{result_folder_name_before}", 'data.yaml')
            data_yaml_path_current = os.path.join(f"{current_directory}{path}{user_name}/{project_idx}/{result_folder_name}", 'data.yaml')
            
            # data.yaml 파일에서 클래스 이름 로드
            with open(data_yaml_path_before, 'r') as file:
                data_yaml_before = yaml.safe_load(file)
            with open(data_yaml_path_current, 'r') as file:
                data_yaml_current = yaml.safe_load(file)
            
            # 레이블 데이터의 클래스 인덱스 변경
            updated_label_data = ""
            for line in label_data.split('\n'):
                if line:
                    parts = line.split(' ')
                    class_index = int(parts[0])
                    class_name = data_yaml_before['names'][class_index]
                    if class_name in data_yaml_current['names']:
                        new_class_index = data_yaml_current['names'].index(class_name)
                        parts[0] = str(new_class_index)
                    updated_label_data += ' '.join(parts) + '\n'
            
            label_data = updated_label_data

        if label_data:
            return jsonify({"labelData": label_data})
        else:
            return "Label file not found.", 404
    except Exception as e:
        app.logger.error(e)
        return f"An error occurred: {e}", 500   

@app.route('/getLabelDataFromEx',methods=['POST'])
def get_label_data_example():
    # 이미지 파일 이름에서 확장자 제거
    data = request.get_json()
    path = data.get('path')
    global current_directory
    print('현재기본경로',current_directory)
        # 역슬래시를 슬래시로 변경
    #current_directory = current_directory.replace("\\", "/")
    label_file_dir=f"{current_directory}{path}/labels"
    image_file_dir=f"{current_directory}{path}/images"
    

    # 이미지 파일 목록 가져오기
    image_files = os.listdir(image_file_dir)
    # 확장자를 제외한 이미지 파일 이름 추출
    image_names = [os.path.splitext(file)[0] for file in image_files]

    # 레이블 파일 목록 가져오기
    label_files = os.listdir(label_file_dir)
    # 확장자를 제외한 레이블 파일 이름 추출
    label_names = [os.path.splitext(file)[0] for file in label_files]

    # # 레이블 파일이 없는 이미지 파일에 대해 레이블 파일 생성
    # for image_name in image_names:
    #     if image_name not in label_names:
    #         # 새 레이블 파일 경로 생성
    #         new_label_file = os.path.join(label_file_dir, f"{image_name}.txt")
    #         # 빈 레이블 파일 생성
    #         with open(new_label_file, 'w') as file:
    #             pass  # 빈 파일 생성


    label_files_list= os.listdir(label_file_dir)
    label_data_list = []

    for label_file in label_files_list:
        file_path = os.path.join(label_file_dir, label_file)
        with open(file_path, 'r') as file:
            label_data = file.read().strip()  # 파일 내용 읽기 및 공백 제거
            label_data_list.append(label_data)  # 리스트에 추가

    path = path.lstrip('/')
    yaml_file_path = os.path.join(current_directory, path, "data.yaml").replace('\\','/')

    # YAML 파일 읽기
    with open(yaml_file_path, 'r') as file:
        data = yaml.safe_load(file)

    # classname list 가져오기
    class_names_list = data.get('names', [])
    print("모델 결과 이미지 예시 class_names_list : ",class_names_list)

    try:
        if len(label_data_list)==len(image_names):
            return jsonify({"labelDataList":label_data_list, "classNames":class_names_list}) 
        else:
            return " label파일 수와 이미지 파일 수가 일치하지 않습니다.", 404
    except Exception as e:
        return f"An error occurred: {e}", 500
if __name__ == '__main__':
    app.run(host='127.0.0.1',port=5000,debug=True) 