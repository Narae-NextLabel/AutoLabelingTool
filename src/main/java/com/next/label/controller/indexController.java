package com.next.label.controller;

import java.io.File;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.next.label.Entity.*;

import com.next.label.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class indexController {
    @Autowired
    private EmailService emailService;
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
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/")
    public String index() {

        return "index";
    }

    @GetMapping("/billing")
    public String billing() {
        return "predictLoading";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "notifications";
    }

    @GetMapping("/goProfile")
    public String goProfile() {
        return "profile";
    }

    @GetMapping("/goSignIn")
    public String sign_in() {
        return "signIn";
    }

    @GetMapping("/goSignUp")
    public String sign_up() {
        return "signUp";
    }

    @RequestMapping("/goStart")
    public String classList(Model model) throws JsonProcessingException {
        List<tModel> modelList = modelRepository.findAll();
        List<startDTO> start = new ArrayList<>();
        for (tModel modelClass : modelList) {
            List<tClass> classList = classRepository.findByModelIdx(modelClass);
            if (!classList.isEmpty()) {
                startDTO startDTO = new startDTO();
                startDTO.setModelIdx(modelClass.getModelIdx());
                startDTO.setModelName(modelClass.getModelName());
                List<String> classNames = classList.stream()
                        .map(tClass::getClassName)
                        .collect(Collectors.toList());
                System.out.println("!!!!!!!!!!!classList: "+classList);
                System.out.println("!!!!!!!!!!!classNames: "+classNames);
                startDTO.setClassNames(classNames);
                startDTO.setClassCount(classNames.size());
                startDTO.setFirstClassName(classNames.get(0));

                start.add(startDTO);

                String classNamesJson = objectMapper.writeValueAsString(classNames);
                model.addAttribute("classNames", classNamesJson);
            }
        }
        List<tClass> tClass = classRepository.findAll();
        model.addAttribute("tClass", tClass);

        model.addAttribute("start", start);
        return "start";
    }

    @PostMapping("/goAnnotation")
    public String virtual_reality(Model model, HttpServletRequest request,@RequestParam(name = "projectIdx") String projectIdx,@RequestParam("labelFolder") String labelFolder)  {
        HttpSession session = request.getSession();
        tUser userInfo = (tUser) session.getAttribute("userInfo");
        System.out.println(labelFolder);
        model.addAttribute("userName",userInfo.getUserName());
        model.addAttribute("projectIdx",projectIdx);
        model.addAttribute("labelFolder",labelFolder);
        return "annotation";
    }

    predictDTO predict1 = new predictDTO();

    @GetMapping("/predictLoading")
    public String predict_load(Model model, HttpServletRequest request, @RequestParam(name = "projectIdx") String projectIdx, @RequestParam(name = "userName") String userName) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(userName);
        model.addAttribute("projectIdx", projectIdx);
        model.addAttribute("userName", userName);
        String flaskEndpoint = "http://localhost:5000/predict";
        String imagePath = "/src/main/resources/static/";
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("imagePath", imagePath);
        requestData.put("projectIdx", projectIdx);
        requestData.put("userName", userName);
        // Flask에서 반환된 문자열을 받음
        String response = restTemplate.postForObject(flaskEndpoint, requestData, String.class);
        model.addAttribute("response", response);
        // 반환된 값을 로그에 출력하거나 다른 방식으로 처리
        System.out.println("첫 predict Flask 응답: " + response);
        HttpSession session = request.getSession();
        tUser userInfo = (tUser) session.getAttribute("userInfo");
        emailService.sendCompletionEmail(userInfo.getUserId(),
                "예측 이미지 라벨링이 완료되었습니다!",
                "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" background=\"#fbfbfb\" style=\"padding: 20px 16px 82px; color: #191919; font-family: 'Noto Sans KR', sans-serif;\" class=\"wrapper\">\n" +
                        "        <tbody style=\"display: block; max-width: 600px; margin: 0 auto;\">\n" +
                        "          <tr width=\"100%\" style=\"display: block;\">\n" +
                        "            <td width=\"100%\" style=\"display: block;\">\n" +
                        "              \n" +
                        "              <table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bgColor=\"#FFFFFF\" style=\"display: inline-block; padding: 32px; text-align: left; border-top: 3px solid #353535;border-bottom: 3px solid #353535; border-collapse: collapse;\" class=\"container\">\n" +
                        "                <tbody style=\"display: block;\">\n" +
                        "                 \n" +
                        "                \n" +
                        "                  <tr width=\"100%\" style=\"display: block; margin-bottom: 32px;\">\n" +
                        "                    <td width=\"100%\" style=\"display: block;\">\n" +
                        "                       <img height=\"60\" style=\"margin-left: 220px;  vertical-align: middle; display: inline-block; padding: 0 5px 0 0;\" src=\"https://blogfiles.pstatic.net/MjAyMzEyMTJfMTg5/MDAxNzAyMzUzNjg5ODY2.euixFF-oCaPGXYmVUDMkIWxWX7597HydZRSJvTA2ZUQg.09HlTfgv4J61UcBA2089CYEkFZBmR7aW7uSRuUMleeQg.PNG.mangokong11/logoN.png\" />" +
                        "                      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" bgColor=\"#F8F9FA\" style=\"padding: 40px 20px; border-radius: 4px;background:white\"  class=\"content\">\n" +
                        "                        <tbody style=\"display: block;\">\n" +
                        "                            \n" +
                        "                          <tr style=\"display: block;text-align: left;\">\n" +
                        "                            NEXT LABEL을 이용해주셔서 감사합니다!\n" +
                        "                            <br>\n" +
                        "                            <br>\n" +
                        "                            해당 프로젝트 이미지들의 예상 라벨링이 완료되었습니다\n" +
                        "                            <br><br>\n" +
                        "                            NEXT LABEL로 돌아오셔서 확인해보세요!\n" +
                        "                          </tr>\n" +
                        "                        </tbody>\n" +
                        "                      </table>\n" +
                        "                    </td>\n" +
                        "                  </tr>\n" +
                        "                  <!-- 발신전용 & 저작권 -->\n" +
                        "                  <tr>\n" +
                        "                    발신전용 메일입니다.\n" +
                        "                  </tr>\n" +
                        "                  <tr>\n" +
                        "                    <td style=\"padding-bottom: 24px; color: #A7A7A7; font-size: 12px; line-height: 20px;\"> 2023 NEXT LABEL. All Rights Reserved.</td>\n" +
                        "                  </tr>\n" +
                        "                  <!-- 푸터(통합 서비스) -->\n" +
                        "                  <tr width=\"100%\" style=\"display:block; padding-top: 24px; border-top: 1px solid #e9e9e9;\">\n" +
                        "                    <td style=\"position: relative;\">\n" +
                        "                      <img height=\"16\" style=\"vertical-align: middle; display: inline-block; padding: 0 5px 0 0;\" src='https://d3hqehqh94ickx.cloudfront.net/images/mail-asset/lvup_gray.png' />\n" +
                        "                      <img height=\"10\" style=\"display: inline-block; border-left: 1px solid #E9E9E9; padding: 0 8px;\" src='https://d3hqehqh94ickx.cloudfront.net/images/mail-asset/gco_gray.png' />\n" +
                        "                    \n" +
                        "                    </td>\n" +
                        "                  </tr>\n" +
                        "                </tbody>\n" +
                        "              </table>\n" +
                        "            </td>\n" +
                        "          </tr>\n" +
                        "        </tbody>\n" +
                        "      </table>"
        );
        return "predictLoading";
    }

    @PostMapping("/receiveTrainData") // train 이후 성공적으로 반환값 전송
    public String handleTrainingData(@RequestBody TrainingDTO trainingDTO, Model model) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        // trainingData 객체를 사용하여 필요한 작업 수행
        try {
            // List를 JSON 문자열로 변환

            String projectIdx = predict1.getProjectIdx();
            String userName = predict1.getUserName();
            String resultFolderName = predict1.getResultFolderName();
            // 모델에 추가
            model.addAttribute("projectIdx", projectIdx);
            model.addAttribute("resultFolderName", resultFolderName);
            model.addAttribute("userName", userName);

            String flaskEndpoint = "http://localhost:5000/predictAfterTrain";
            String imagePath = "/src/main/resources/static/";
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("projectIdx", projectIdx);
            requestData.put("userName", userName);
            // Flask에서 반환된 문자열을 받음
            String trainPredictResponse = restTemplate.postForObject(flaskEndpoint, requestData, String.class);

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(trainPredictResponse);
            String[] trainClassNamesArray = objectMapper.convertValue(rootNode.get("trainClassNames"), String[].class);
            String[] fileNamesArray = objectMapper.convertValue(rootNode.get("fileNames"), String[].class);
            String[] imagePathListArray = objectMapper.convertValue(rootNode.get("imagePathList"), String[].class);
            System.out.println("성공시trainClassNames" + trainClassNamesArray);
            System.out.println("성공시fileNames" + fileNamesArray);
            System.out.println("성공시imagePathList" + imagePathListArray);
            ;

            model.addAttribute("trainClassNames", trainClassNamesArray);
            model.addAttribute("fileNames", fileNamesArray);
            model.addAttribute("imagePathList", imagePathListArray);
            // 다른 로직 수행...

            return "predictAfterTrain";
        } catch (Exception e) {
            // JSON 변환 중 에러 처리
            e.printStackTrace();
            return "pageMoveTest"; //  에러 페이지로 리디렉트
        }
    }

    @GetMapping("/goPredictAfterTrain")
    public String goPredictAfterTrain() {
        return "predictAfterTrain";
    }


    @GetMapping("/pageMoveTest")
    public String testPageMove() {
        return "pageMoveTest";
    }

    @GetMapping("/goWorkspace")
    public String goWorkspace(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        tUser userInfo = (tUser) session.getAttribute("userInfo");
        List<tProject> projectList = projectRepository.findByUserId(userInfo);

        // 내림차순
        List<tProject> sortedProjects = projectList.stream()
                .sorted((p1, p2) -> p2.getUploadedAt().compareTo(p1.getUploadedAt()))
                .collect(Collectors.toList());


        List<workspaceDTO> workspace = new ArrayList<>();

        for (tProject project : sortedProjects) {
            List<tUpload> projectUploads = uploadRepository.findByProjectIdx(project);
            if (!projectUploads.isEmpty()) {
                String imagePath = String.format("%s/%s/image/%s", userInfo.getUserName(), project.getProjectIdx(), projectUploads.get(0).getFileOriName());

                // Timestamp를 Date로 변환 후 포맷팅
                Date uploadedDate = new Date(project.getUploadedAt().getTime());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String formattedTime = dateFormat.format(uploadedDate);

                workspaceDTO workspaceDTO = new workspaceDTO(project.getProjectName(), imagePath, formattedTime, project.getProjectIdx().toString());
                workspace.add(workspaceDTO);
            }
        }

        model.addAttribute("workspace", workspace);
        System.out.println(userInfo);

        return "workspace";
    }


    @RequestMapping("/signUp")
    public String signUp(tUser tUser, String userPw2, Model model) {
        // ID 중복확인
        Optional<tUser> existingUser = userRepository.findByUserId(tUser.getUserId());
        Optional<tUser> existingUserByName = userRepository.findByUserName(tUser.getUserName());

        if (tUser.getUserId() == null || tUser.getUserPw() == null || tUser.getUserName() == null || !userPw2.equals(tUser.getUserPw())) {
            model.addAttribute("signUpError2", "아이디와 비밀번호 아이디를 모두 입력해주세요.");
            return "signUp";
        } else if (existingUser.isPresent()) {
            System.out.println("이미 존재하는 ID입니다!");
            model.addAttribute("signUpError", "이미 존재하는 ID입니다.");
            return "signUp";
        } else if (existingUserByName.isPresent()) {
            System.out.println("이미 존재하는 이름입니다.");
            model.addAttribute("signUpError3", "이미 존재하는 이름입니다.");
            return "signUp";
        } else {
            userRepository.save(tUser);
            System.out.println("회원가입 성공, signIn으로 redirect");
            return "signIn";
        }
    }


    @RequestMapping("/signIn")
    public String signIn(tUser tUser, Model model, HttpServletRequest request) {

        tUser userInfo = userRepository.findByUserIdAndUserPw(tUser.getUserId(), tUser.getUserPw());
        HttpSession session = request.getSession();

        System.out.println("!!!!!!!!!!!!!" + userInfo);

        if (tUser.getUserId() != null && tUser.getUserPw() != null) {
            if (userInfo != null) {
                System.out.println("로그인 성공");
                System.out.println("userInfo" + userInfo);
                session.setAttribute("userInfo", userInfo);
                return "redirect:/";
            } else {
                System.out.println("로그인 실패");
                model.addAttribute("logInError", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
        }
        return "signIn";
    }

    @RequestMapping("/logOut")
    public String logOut(HttpSession session) {
        session.removeAttribute("userInfo");
        System.out.println("로그아웃 성공!");
        return "redirect:/";
    }
    @RequestMapping("/goUpload")
    public String goUpload(Model model, tModel modelName) {
        model.addAttribute("modelName", modelName);
        return "upload";
    }

    @RequestMapping("/goUpload2")
    public String goUpload2() {
        return "upload2";
    }

    @RequestMapping("/goWorkspace2")
    public String goWorkspace2(tProject projectIdx, Model model) {
        List<tUpload> uploadList = uploadRepository.findByProjectIdx(projectIdx);
        // 이미지와 비디오를 분리
        List<tUpload> imageList = new ArrayList<>();
        List<tUpload> videoList = new ArrayList<>();
        boolean hasVideo = false; // 비디오가 있는지 여부를 확인하기 위한 변수

        // Set flag to distinguish between image and video
        for (tUpload upload : uploadList) {
            if (isVideo(upload.getFileOriName())) {
                upload.setFileOriName(addPredictSuffix(upload.getFileOriName()));
                videoList.add(upload);
                hasVideo = true;
            } else if (isImage(upload.getFileOriName())) {
                imageList.add(upload);
            }
        }

        // 이미지만 보내는 경우
        if (hasVideo) {
            model.addAttribute("imageList", new ArrayList<>()); // 비디오가 있으면 빈 이미지 리스트 설정
            model.addAttribute("videoList", videoList);
        } else {
            model.addAttribute("imageList", imageList);
            model.addAttribute("videoList", new ArrayList<>()); // 이미지만 있으면 빈 비디오 리스트 설정
        }
        model.addAttribute("projectIdx", projectIdx);
        return "Workspace2";
    }

    // 파일 확장자를 기반으로 이미지 여부를 판단하는 메서드
    private boolean isImage(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".JPG") || fileName.endsWith(".PNG") || fileName.endsWith(".JPEG");
    }

    // 파일 확장자를 기반으로 비디오 여부를 판단하는 메서드
    private boolean isVideo(String fileName) {
        return fileName.endsWith(".mp4") || fileName.endsWith(".MOV") || fileName.endsWith(".avi") || fileName.endsWith(".MP4") || fileName.endsWith(".mov");
    }

    private String addPredictSuffix(String fileName) {
        // Add "_predict" only if it doesn't already end with "_predict"
        if (!fileName.endsWith("_predict.mp4")) {
            return fileName.replace(".mp4", "_predict.webm");
        } else if (!fileName.endsWith("_predict.MOV")) {
            return fileName.replace(".MOV", "_predict.webm");
        } else if (!fileName.endsWith("_predict.avi")) {
            return fileName.replace(".avi", "_predict.webm");
        } else if (!fileName.endsWith("_predict.MP4")) {
            return fileName.replace(".MP4", "_predict.webm");
        } else if (!fileName.endsWith("_predict.mov")) {
            return fileName.replace(".mov", "_predict.webm");
        }
        return fileName;
    }

    @GetMapping("/goLabelingEx")
    public String goLabelingEx(@RequestParam String images, @RequestParam String modelName, Model model) {
        try {
            List<String> imageList = objectMapper.readValue(images, List.class);// JSON 문자열을 리스트로 변환
            model.addAttribute("modelName",modelName);
            model.addAttribute("images", imageList);
        }
        catch (Exception e) {
            e.printStackTrace();
            // 에러 처리
        }
        return "labelingEx";
    }

    @RequestMapping("/uploadImg")
    @Transactional  // 트랜잭션 설정 // 여기에서 Flask 경로 지정
    public String uploadImg(tUpload tUpload, MultipartFile[] file, HttpServletRequest request, tProject tProject, RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession();

        tUser userInfo = (tUser) session.getAttribute("userInfo");
        System.out.println(userInfo);

        tUser userForProject = new tUser();


        userForProject.setUserId(userInfo.getUserId());
        System.out.print(userInfo.getUserId()); // 이게 유저 아이디

        tProject.setUserId(userForProject);
        projectRepository.save(tProject);
        final String PATH = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\" + userInfo.getUserName() + "\\" + tProject.getProjectIdx() + "\\image\\";

        System.out.println("프로젝트번호" + tProject.getProjectIdx());
        UUID uuid = UUID.randomUUID();


        try {
            File directory = new File(PATH);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            for (int i = 0; i < file.length; i++) {

                String filename = uuid + "_" + file[i].getOriginalFilename();
                tUpload.setFileName(file[i].getOriginalFilename());

                System.out.println("파일네임2" + file[i].getOriginalFilename());
                System.out.println("uuid파일네임" + filename);

                File f = new File(PATH + filename);

                file[i].transferTo(f);

                tUpload.setFileOriName(filename);

                tUpload.setProjectIdx(tProject);

                uploadRepository.save(tUpload);
            }
            redirectAttributes.addAttribute("projectIdx", tProject.getProjectIdx());
            redirectAttributes.addAttribute("userName", userInfo.getUserName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/predictLoading";
    }


    @GetMapping("/afterTrain")
    public String afterTrainFail(Model model, @RequestParam(name = "projectIdx") String projectIdx, @RequestParam(name = "userName") String userName, @RequestParam(name = "lastModelIdx") String lastModelIdx) {
        // 학습 후 데이터 전송 실패 시
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Optional<tUser> user = userRepository.findByUserName(userName);
            if (user.isPresent()) {
                model.addAttribute("userId", user.get().getUserId());
            } else {
                // 해당 userName에 대한 User가 없는 경우의 처리
                // 에러 메시지를 모델에 추가하거나 다른 처리를 수행
            }
            // List를 JSON 문자열로 변환
            // 모델에 추가
            model.addAttribute("projectIdx", projectIdx);
            model.addAttribute("userName", userName);

            String flaskEndpoint = "http://localhost:5000/predictAfterTrain";
            String imagePath = "/src/main/resources/static/";
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("projectIdx", projectIdx);
            requestData.put("userName", userName);
            requestData.put("lastModelIdx", lastModelIdx);
            // Flask에서 반환된 문자열을 받음
            String trainPredictResponse = restTemplate.postForObject(flaskEndpoint, requestData, String.class);
            JsonNode rootNode = objectMapper.readTree(trainPredictResponse);
            System.out.println(trainPredictResponse);
            String modelName = objectMapper.convertValue(rootNode.get("modelName"), String.class);
            String resultFolderName = objectMapper.convertValue(rootNode.get("resultFolderName"), String.class);
            model.addAttribute("trainPredictResponse", trainPredictResponse);
            model.addAttribute("resultFolderName", resultFolderName);
            model.addAttribute("modelName", modelName);

            System.out.println("학습 이후 예측 시_modelName :" + modelName);

            // JSON 파싱

            String[] trainClassNamesArray = objectMapper.convertValue(rootNode.get("trainClassNames"), String[].class);
            String[] fileNamesArray = objectMapper.convertValue(rootNode.get("fileNames"), String[].class);
            String[] imagePathListArray = objectMapper.convertValue(rootNode.get("imagePathList"), String[].class);
            System.out.print("실패시trainClassNames: ");
            for (String className : trainClassNamesArray) {
                System.out.print(className + ", ");
            }
            System.out.println();

            System.out.print("실패시fileNames: ");
            for (String fileName : fileNamesArray) {
                System.out.print(fileName + ", ");
            }
            System.out.println();

            System.out.print("실패시imagePathList: ");
            for (String image_path : imagePathListArray) {
                System.out.print(image_path + ", ");
            }
            System.out.println();

            model.addAttribute("trainClassNames", trainClassNamesArray);
            model.addAttribute("fileNames", fileNamesArray);
            model.addAttribute("imagePathList", imagePathListArray);

            return "predictAfterTrain";
        } catch (Exception e) {
            // JSON 변환 중 에러 처리
            e.printStackTrace();
            return "redirect:/goWorkspace"; //  에러 페이지로 리디렉트
        }
    }


}
