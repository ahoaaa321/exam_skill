package com.demo.module.exam.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.demo.module.exam.repository.MaterialRepository;

/**
 * 报名材料与审核进度：所有访问先做归属权校验（本人或管理/考务/超管）。
 */
@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public void addMaterial(Long registrationId, String fileName, String filePath, long fileSize, String fileType,
                            Long currentUserId, int currentRole) {
        ensureOwner(registrationId, currentUserId, currentRole);
        materialRepository.insert(registrationId, fileName, filePath, fileSize, fileType);
    }

    public List<Map<String, Object>> getMaterials(Long registrationId, Long currentUserId, int currentRole) {
        ensureOwner(registrationId, currentUserId, currentRole);
        return materialRepository.findByRegistration(registrationId);
    }

    public void deleteMaterial(Long id, Long currentUserId, int currentRole) {
        Long regId = materialRepository.findRegistrationIdByMaterial(id);
        if (regId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "材料不存在");
        }
        ensureOwner(regId, currentUserId, currentRole);
        materialRepository.delete(id);
    }

    /** 管理/考务/超管查看任意报名的审核记录 */
    public List<Map<String, Object>> getAuditRecords(Long registrationId) {
        return materialRepository.findAuditRecords(registrationId);
    }

    /** 考生查看本人报名审核进度（含归属权校验） */
    public List<Map<String, Object>> getTimeline(Long registrationId, Long currentUserId, int currentRole) {
        ensureOwner(registrationId, currentUserId, currentRole);
        return materialRepository.findAuditRecords(registrationId);
    }

    /** 材料归属校验：本人或管理/考务/超管可操作 */
    private void ensureOwner(Long registrationId, Long currentUserId, int currentRole) {
        Long ownerId = materialRepository.findRegistrationUserId(registrationId);
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        if (currentRole == 0 && !ownerId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作他人报名材料");
        }
    }
}
