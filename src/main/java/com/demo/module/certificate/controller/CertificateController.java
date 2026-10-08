package com.demo.module.certificate.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.security.RequireRole;
import com.demo.module.certificate.service.CertificateService;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /** 考生查看本人证书 */
    @GetMapping("/certificates/my")
    public Result<List<Map<String, Object>>> mine() {
        return Result.ok(certificateService.myCertificates(UserContext.currentUserId()));
    }

    /** 公开证书查验（无需登录） */
    @GetMapping("/public/certificates/verify")
    public Result<Map<String, Object>> verify(@RequestParam("no") String certificateNo) {
        return Result.ok(certificateService.verify(certificateNo));
    }

    /** 管理端证书列表 */
    @GetMapping("/admin/certificates")
    @RequireRole({1, 3})
    public Result<List<Map<String, Object>>> all() {
        return Result.ok(certificateService.all());
    }

    /** 发放证书 */
    @PostMapping("/admin/certificates")
    @RequireRole({1, 3})
    public Result<Void> issue(@RequestBody Map<String, Object> body) {
        Long registrationId = ((Number) body.get("registrationId")).longValue();
        String certificateNo = (String) body.get("certificateNo");
        String tradeName = (String) body.get("tradeName");
        String levelName = (String) body.get("levelName");
        String issuer = (String) body.getOrDefault("issuer", "职业技能鉴定中心");
        certificateService.issue(registrationId, certificateNo, tradeName, levelName, issuer);
        return Result.ok();
    }

    /** 作废证书 */
    @PutMapping("/admin/certificates/{id}/revoke")
    @RequireRole({1, 3})
    public Result<Void> revoke(@PathVariable Long id) {
        certificateService.revoke(id);
        return Result.ok();
    }
}
