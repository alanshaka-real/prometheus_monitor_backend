package com.wenmin.prometheus.module.distribute.service;

import com.wenmin.prometheus.module.distribute.entity.PromDistributeSoftware;
import com.wenmin.prometheus.module.distribute.vo.SoftwareDownloadVO;
import com.wenmin.prometheus.module.distribute.vo.SoftwareUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SoftwareService {

    Map<String, Object> listSoftware(String name, String osType, String osArch);

    int scanDirectory();

    PromDistributeSoftware findSoftware(String name, String osType, String osArch);

    String getSoftwareFilePath(PromDistributeSoftware software);

    String downloadLatest(List<String> components);

    SoftwareDownloadVO getDownloadStatus(String downloadId);

    SoftwareUploadVO uploadSoftware(MultipartFile file);
}
