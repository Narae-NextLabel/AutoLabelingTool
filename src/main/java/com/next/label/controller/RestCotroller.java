package com.next.label.controller;

import com.next.label.Entity.*;
import com.next.label.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.InputStream;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;

import java.util.stream.Collectors;

import java.nio.file.Paths;
import java.util.*;

@RestController
public class RestCotroller {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private ModelRepository modelRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UploadRepository uploadRepository;

    @RequestMapping("/getClassNamesByModelIdx")
    public List<String> getClassNamesByModelIdx(Long modelIdx) {
        System.out.println("!!!!!!!!!"+modelIdx);
        return classRepository.findClassNameByModelIdx(modelIdx);
    }
    @RequestMapping("/lastModelIdx")
    public String getLastModelIdx() {
        Long lastModelIdx = modelRepository.findLastModelIdx(); // findLastModelIdx() 메소드의 반환형이 Long
        System.out.println("restController lastModeIdx" + lastModelIdx);
        String stringLastModelIdx=lastModelIdx+"";

        return stringLastModelIdx;
    }
    @RequestMapping("findModelByClassName")
    public List<startDTO> findModelByClassName(String className) {
        System.out.println("!!!!!!!!className: " + className);
        List<startDTO> startDTOList  = new ArrayList<>();
        List<tClass> classList = classRepository.findAllByClassName(className);
        System.out.println("!!!!!!!!classList: " + classList);
        for (tClass tClass : classList) {
            tModel classModel = tClass.getModelIdx();
            System.out.println("!!!!!!!!modelIdx: " + classModel);

            // 모델에 대응하는 모든 클래스를 가져옵니다.
            List<tClass> modelClasses = classRepository.findByModelIdx(classModel);
            List<String> classNames = modelClasses.stream()
                    .map(com.next.label.Entity.tClass::getClassName)
                    .collect(Collectors.toList());

            startDTO startDTO = new startDTO();
            startDTO.setModelIdx(classModel.getModelIdx());
            startDTO.setModelName(classModel.getModelName());
            startDTO.setClassNames(classNames);
            startDTO.setClassCount(classNames.size());
            startDTO.setFirstClassName(classNames.get(0));

            startDTOList.add(startDTO);

//            tModel modelList = modelRepository.findAllByModelIdx(classModelIdx.getModelIdx());
//            System.out.println("!!!!!!!!modelList: " + modelList);

            // modelNameList.add(new startDTO(modelList.getModelIdx(), modelList.getModelName(), tClass.getClassName(), tClass.getClassIconPath()));

        }
        return startDTOList;

    }

    @RequestMapping("/loadImages")
    public Map<String, List<String>> loadImages(@RequestParam("projectIdx") Long projectIdx, // projectIdx를 Long으로 받도록 수정
                                                @RequestParam("userName") String userName){

        System.out.println("이미지들 가져오기!!!");
        tProject tProject = new tProject();
        tProject.setProjectIdx(projectIdx);
        List<tUpload> tUploads=uploadRepository.findByProjectIdx(tProject);
        List<String> imagesPath = new ArrayList<>();
        List<String> imagesName = new ArrayList<>();
        for (tUpload tUpload : tUploads){
            String imagePath = String.format("%s/%s/image/%s", userName, projectIdx, tUpload.getFileOriName());
            imagesPath.add(imagePath);
            imagesName.add(tUpload.getFileOriName());
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("imagesPath", imagesPath);
        result.put("imagesName", imagesName);

        return result;
    }
    @RequestMapping("/saveVideoData")
    public ResponseEntity<String> saveVideoData(@RequestBody videoDTO videoDTO) {
        // 여기에 요청 처리 로직을 추가
        // videoDataRequest에서 필요한 데이터 추출
        // 예시: 프로젝트 인덱스와 파일 이름 출력

        System.out.println("Project Index: " + videoDTO.getProjectIdx());
        System.out.println("File Names: " + String.join(", ", videoDTO.getFileName()));

        // Find the existing tProject entity by projectIdx
        Optional<tProject> optionalProject = projectRepository.findById(videoDTO.getProjectIdx());

        if (optionalProject.isPresent()) {
            // If the project exists, retrieve it
            tProject existingProject = optionalProject.get();

            // Loop through file names and save tUpload entities
            for (String filename : videoDTO.getFileName()) {
                // Create a new tUpload entity
                tUpload tUpload = new tUpload();

                // Set the values for the tUpload entity
                tUpload.setFileName(filename);
                tUpload.setFileOriName(filename);

                // Set the existing project to tUpload
                tUpload.setProjectIdx(existingProject);

                // Save the tUpload entity to the database
                uploadRepository.save(tUpload);
            }

            // Generate response according to processing results
            return ResponseEntity.ok("Success");
        } else {
            // If the project does not exist, you might want to handle this case accordingly
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");
        }}




    @RequestMapping("/deleteProject")
    public String deleteProject(Long projectIdx, HttpServletRequest request){
        System.out.println("!!!!!!!!!!!!!"+projectIdx);
        HttpSession session = request.getSession();
        tUser userInfo = (tUser) session.getAttribute("userInfo");
        Path path = Paths.get(System.getProperty("user.dir")+"\\src\\main\\resources\\static\\"+userInfo.getUserName()+"\\"+projectIdx+"\\");


        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
            // 파일 삭제 실패 시 예외 처리
        }
        projectRepository.deleteById(projectIdx);

        return "redirect:/goWorkspace";
    }
    @Transactional
    @RequestMapping("/updateProjectName")
    public String updateProjectName(Long projectIdx, String projectName){
        projectRepository.updateProjectName(projectIdx,projectName);

        return "redirect:/goWorkspace";
    }
    @RequestMapping("/deletefileOriName")
    public String deletefileOriName(String fileOriName,Long projectIdx, HttpServletRequest request) throws IOException {
        System.out.println("!!!!!!!!!!!!!"+fileOriName);
        System.out.println("!!!!!!!!!!!!!"+projectIdx);

        HttpSession session = request.getSession();
        tUser userInfo = (tUser) session.getAttribute("userInfo");
        List<Path> pathsToDelete = new ArrayList<>();
        String directoryPath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\" + userInfo.getUserName() + "\\" + projectIdx + "\\";
        List<File> allResultFolders = getResultFolders(directoryPath);
        System.out.println("삭제되는 result폴더 내 txt파일 : "+allResultFolders.size()+"개");
        for (File resultFolder : allResultFolders) {
            String updatedFileName = fileOriName.replaceFirst("\\.[^.]+$", "." + "txt");
            Path path2 = Paths.get(resultFolder.getAbsolutePath() + "\\labels\\" + updatedFileName + "\\");
            pathsToDelete.add(path2);
        }
        Path path = Paths.get(System.getProperty("user.dir")+"\\src\\main\\resources\\static\\"+userInfo.getUserName()+"\\"+projectIdx+"\\image\\"+fileOriName+"\\");

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            for (Path pathToDelete : pathsToDelete) {
                Files.walk(pathToDelete)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 파일 삭제 실패 시 예외 처리
        }
        uploadRepository.deleteById(fileOriName);

        return "redirect:/goWorkspace2";
    }
    private static List<File> getResultFolders(String directoryPath) throws IOException {
        List<File> recentResultFolders = Files.walk(Paths.get(directoryPath))
                .filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("result"))
                .sorted(Comparator.comparingLong(path -> {
                    try {
                        return Files.readAttributes((Path) path, BasicFileAttributes.class).creationTime().toMillis();
                    } catch (IOException e) {
                        throw new RuntimeException("파일 속성 읽기 오류", e);
                    }
                }).reversed())
                .map(Path::toFile)
                .collect(Collectors.toList());

        return recentResultFolders;
    }
    private static List<File> getRecentResultFolders(String directoryPath, int count) throws IOException {
        List<File> recentResultFolders = Files.walk(Paths.get(directoryPath))
                .filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("result"))
                .sorted(Comparator.comparingLong(path -> {
                    try {
                        return Files.readAttributes((Path) path, BasicFileAttributes.class).creationTime().toMillis();
                    } catch (IOException e) {
                        throw new RuntimeException("파일 속성 읽기 오류", e);
                    }
                }).reversed())
                .limit(count)
                .map(Path::toFile)
                .collect(Collectors.toList());

        return recentResultFolders;
    }

    @RequestMapping("/insertModel")
    public tModel addModel(tModel model) {
        System.out.println(model);
        // 모델 데이터를 저장합니다.
        tModel savedModel = modelRepository.save(model);
        return savedModel; //HTTP 상태 코드 201은 "Created"를 의미, 성공적으로 생성되었음을 나타냄, savedModel은 DB에 저장된 엔터티 값이 있고 보통 JSON형식으로 불러와짐
    }

    @RequestMapping("/insertClass")
    public ResponseEntity<?> addClass(@RequestParam Long modelIdx, @RequestParam List<String> classNames) {
        Optional<tModel> modelOptional = modelRepository.findById(modelIdx);
        if (!modelOptional.isPresent()) {
            return new ResponseEntity<>("Model not found", HttpStatus.NOT_FOUND);
        }

        tModel model = modelOptional.get();
        List<tClass> savedClasses = new ArrayList<>();
        System.out.println("addClass에 들어온 classNames :"+classNames);
        for (String className : classNames) {
            tClass newClass = new tClass();
            newClass.setModelIdx(model);
            newClass.setClassName(className);
            tClass savedClass = classRepository.save(newClass);
            savedClasses.add(savedClass);
        }

        System.out.println("savedClasses: " + savedClasses);
        return new ResponseEntity<>("Classes Saved", HttpStatus.CREATED);
    }

    @RequestMapping("/findUserId")
    public ResponseEntity<String> findUserIdByUserName(String userName) {
        Optional<tUser> user = userRepository.findByUserName(userName);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get().getUserId());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/getImagesfromStart")
    public List<String> getImageList(String modelName) {
        File folder = new File("src/main/resources/static/assets/labelingEx/"+modelName+"/images");
        System.out.println("getImagesfromStart에서 받은 modelName: "+modelName);
        return Arrays.stream(folder.listFiles())
                .filter(file -> file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")))
                .map(File::getName)
                .collect(Collectors.toList());
    }



    @RequestMapping("/downloadProject")
    public ResponseEntity<FileSystemResource> downloadProject(Long projectIdx,HttpServletRequest request){
        try {
            HttpSession session = request.getSession();
            tUser userInfo = (tUser) session.getAttribute("userInfo");

            String projectFolderPath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\" + userInfo.getUserName() + "\\" + projectIdx+ "\\";
            String zipFileName = "project.zip";
            String zipFilePath = System.getProperty("user.home") +"\\Desktop\\" + zipFileName;
            ZipUtil.zipDirectory(projectFolderPath, zipFilePath);

            File zipFile = new File(zipFilePath);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(zipFile));
        } catch (Exception e) {
            // 예외 처리
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }


    @RequestMapping("/getBoundingBoxes")
    public ResponseEntity<Map<String, Object>> getBoundingBoxes(tProject projectIdx, String resultType, HttpServletRequest request, Model model) throws IOException {
        System.out.println("!!!!!!!!!!!!!!!!"+projectIdx);
        System.out.println("!!!!!!!!!!!!!!!!"+resultType);

        HttpSession session = request.getSession();
        tUser userInfo = (tUser) session.getAttribute("userInfo");

        String directoryPath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\" + userInfo.getUserName() + "\\" + projectIdx.getProjectIdx()+ "\\";
            List<File> recentResultFolders = getRecentResultFolders(directoryPath, 2);

            System.out.println("가장 최근에 생성된 " + recentResultFolders.size() + "개의 result 폴더:");
            System.out.println(recentResultFolders);
        Yaml yaml = new Yaml();
        try {
            if(resultType==null) {
                File labelFolder = new File(recentResultFolders.get(0), "labels");
                model.addAttribute("labelFolder", labelFolder);

                InputStream input = Files.newInputStream(Paths.get(recentResultFolders.get(0) + "\\data.yaml\\"));

                Object yamlData = yaml.load(input);
                System.out.println("!!!!!!!!yamlData:"+yamlData);

                System.out.println("!!!!!!!!labelFolder:"+labelFolder);

                List<Map<String, Object>> boundingBoxesList = new ArrayList<>();

                for (File file : labelFolder.listFiles()) {
                    if (file.isFile()) {
                        String fileContent = getFileContent(file);

                        List<Map<String, Object>> boundingBoxes = parseBoundingBoxes(fileContent);

                        Map<String, Object> response = new HashMap<>();
                        response.put("fileName", file.getName());
                        response.put("fileContent", fileContent);
                        response.put("boundingBoxes", boundingBoxes);
                        response.put("labelFolder",labelFolder);
                        boundingBoxesList.add(response);
                    }
                }

                Map<String, Object> finalResponse = new HashMap<>();
                finalResponse.put("yamlData", yamlData);
                finalResponse.put("boundingBoxesList", boundingBoxesList);
                return ResponseEntity.ok(finalResponse);
            } else  {
                File labelFolder = new File(recentResultFolders.get(1), "labels");
                model.addAttribute("labelFolder", labelFolder);
                InputStream input = Files.newInputStream(Paths.get(recentResultFolders.get(1) + "\\data.yaml\\"));
                Object yamlData = yaml.load(input);
                System.out.println("!!!!!!!!yamlData:"+yamlData);
                System.out.println(labelFolder);
                List<Map<String, Object>> boundingBoxesList = new ArrayList<>();

                for (File file : labelFolder.listFiles()) {
                    if (file.isFile()) {
                        String fileContent = getFileContent(file);

                        List<Map<String, Object>> boundingBoxes = parseBoundingBoxes(fileContent);

                        Map<String, Object> response = new HashMap<>();
                        response.put("fileName", file.getName());
                        response.put("fileContent", fileContent);
                        response.put("boundingBoxes", boundingBoxes);
                        response.put("labelFolder",labelFolder);
                        boundingBoxesList.add(response);
                    }
                }

                Map<String, Object> finalResponse = new HashMap<>();
                finalResponse.put("yamlData", yamlData);
                finalResponse.put("boundingBoxesList", boundingBoxesList);
                return ResponseEntity.ok(finalResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private String getFileContent(
            File file) throws IOException {
        byte[] bytes = FileCopyUtils.copyToByteArray(file);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Map<String, Object>> parseBoundingBoxes(String fileContent) {
        List<Map<String, Object>> boundingBoxes = new ArrayList<>();

        // 각 줄을 파싱
        for (String line : fileContent.split("\n")) {
            String[] parts = line.split("\\s+"); // 공백 또는 탭으로 분리
            if (parts.length < 5) {
                continue;
            }
            // 바운딩 박스 정보 추출
            Map<String, Object> boundingBox = new HashMap<>();
            boundingBox.put("class", parts[0]);
            boundingBox.put("centerX", Double.parseDouble(parts[1]));
            boundingBox.put("centerY", Double.parseDouble(parts[2]));
            boundingBox.put("width", Double.parseDouble(parts[3]));
            boundingBox.put("height", Double.parseDouble(parts[4]));

            boundingBoxes.add(boundingBox);
        }

        return boundingBoxes;
    }


}
