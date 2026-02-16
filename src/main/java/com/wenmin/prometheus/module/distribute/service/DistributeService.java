package com.wenmin.prometheus.module.distribute.service;

import com.wenmin.prometheus.module.distribute.entity.PromDistributeMachine;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeTask;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeTaskDetail;
import com.wenmin.prometheus.module.distribute.vo.MachineDetectVO;

import java.util.List;
import java.util.Map;

public interface DistributeService {

    // Machine management
    Map<String, Object> listMachines(String status, String keyword);

    PromDistributeMachine createMachine(PromDistributeMachine machine);

    PromDistributeMachine updateMachine(String id, PromDistributeMachine machine);

    void deleteMachine(String id);

    MachineDetectVO detectMachine(String id);

    boolean testSshConnection(String ip, Integer port, String username, String password);

    Map<String, Object> testMachineConnection(String machineId);

    List<MachineDetectVO> batchDetect(List<String> machineIds);

    // Task management
    PromDistributeTask createTask(Map<String, Object> request);

    Map<String, Object> listTasks(String status);

    PromDistributeTask getTask(String id);

    List<PromDistributeTaskDetail> getTaskDetails(String taskId);

    void cancelTask(String taskId);

    void retryTaskDetail(String taskId, String detailId);
}
