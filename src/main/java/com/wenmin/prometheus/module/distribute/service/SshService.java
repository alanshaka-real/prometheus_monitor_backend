package com.wenmin.prometheus.module.distribute.service;

import com.wenmin.prometheus.module.distribute.vo.MachineDetectVO;

public interface SshService {

    boolean testConnection(String host, int port, String username, String password);

    MachineDetectVO detectOS(String host, int port, String username, String password);

    String executeCommand(String host, int port, String username, String password, String command);

    void uploadFile(String host, int port, String username, String password,
                    String localPath, String remotePath);
}
