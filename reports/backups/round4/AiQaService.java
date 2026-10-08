package com.demo.module.system.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.demo.common.UserContext;
import com.demo.module.system.repository.SystemRepository;

/**
 * 智能问答：基于关键词匹配完成问题分类与话术答复，并记录问答日志。
 */
@Service
public class AiQaService {

    private final SystemRepository systemRepository;

    public AiQaService(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    public Map<String, Object> ask(String question) {
        String answer = generateAiAnswer(question);
        String category = classifyQuestion(question);
        Long userId = null;
        try {
            userId = UserContext.currentUserId();
        } catch (Exception ignored) {
            // 问答接口允许匿名访问，无登录上下文时 userId 留空
        }
        try {
            systemRepository.insertAiQaLog(userId, question, answer, category);
        } catch (Exception ignored) {
            // 日志写入失败不影响正常答复
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("category", category);
        return result;
    }

    private String classifyQuestion(String q) {
        if (q.contains("报名") || q.contains("条件") || q.contains("资格")) return "报名咨询";
        if (q.contains("缴费") || q.contains("费用") || q.contains("支付") || q.contains("退款")) return "缴费问题";
        if (q.contains("考场") || q.contains("编排") || q.contains("座位") || q.contains("准考证")) return "考场安排";
        if (q.contains("成绩") || q.contains("分数") || q.contains("合格")) return "成绩查询";
        if (q.contains("证书") || q.contains("发证")) return "证书相关";
        if (q.contains("密码") || q.contains("登录") || q.contains("账号")) return "账号问题";
        return "通用咨询";
    }

    private String generateAiAnswer(String q) {
        if (q.contains("报名条件") || q.contains("报名资格")) {
            return "报名条件通常包括：1）年满16周岁；2）身体健康，能正常参加考试；3）具备相应职业技能基础或相关工作经历。具体条件以各考试计划公布的为准，您可在首页「考试计划」中查看详情。";
        }
        if (q.contains("缴费") || q.contains("支付")) {
            return "缴费流程：审核通过后，登录个人中心→我的报名→点击「缴费」按钮，支持微信支付和支付宝。缴费成功后不可退款，请确认信息无误后再支付。";
        }
        if (q.contains("准考证") || q.contains("考场") || q.contains("座位")) {
            return "考务人员完成考场编排后，系统会自动生成准考证。您可在个人中心→准考证打印中查看考场、座位号并打印准考证。请提前30分钟到达考场，携带有效身份证件。";
        }
        if (q.contains("成绩") || q.contains("分数")) {
            return "考试成绩一般在考试结束后15个工作日内发布。成绩分为理论成绩和实操成绩，综合成绩按权重计算（理论40%+实操60%）。综合成绩≥60分为合格。您可在个人中心→成绩查询中查看。";
        }
        if (q.contains("证书")) {
            return "考试合格后，系统自动生成电子证书，您可在个人中心→我的证书中查看和打印。纸质证书将在考后30个工作日内邮寄到报名时填写的地址。证书可在首页「证书查验」中输入编号核验真伪。";
        }
        if (q.contains("密码") || q.contains("找回") || q.contains("忘记")) {
            return "找回密码有两种方式：1）通过注册邮箱接收验证码重置；2）使用注册时获得的16位账号恢复令牌直接重置。如两种方式均不可用，请联系管理员协助处理。";
        }
        if (q.contains("修改") && q.contains("信息")) {
            return "实名认证信息（姓名、身份证号）不支持在线修改。手机号、邮箱、工作单位等可在个人中心→个人信息中修改。如需变更实名信息，请联系报名机构。";
        }
        if (q.contains("审核") || q.contains("退回")) {
            return "报名审核分为初审和复审两个环节。审核通过后进入缴费环节；如被退回，请根据退回原因补充或修改材料后重新提交。审核进度可在个人中心→我的报名→审核进度中查看。";
        }
        if (q.contains("取消") || q.contains("退考")) {
            return "待审核或审核通过未缴费状态下可取消报名，取消后名额释放。已缴费报名原则上不可退款，请谨慎操作。取消后可在首页重新报名其他计划。";
        }
        if (q.contains("公众号") || q.contains("微信")) {
            return "您可以通过微信扫描平台「联系我们」区域的公众号二维码关注我们，获取最新考试资讯和政策解读。也可拨打咨询电话400-000-2026。";
        }
        return "感谢您的咨询！关于「" + q + "」，建议您：1）查看首页「常见问题」栏目；2）浏览对应考试计划的详情说明；3）如仍有疑问，请拨打咨询电话400-000-2026或通过公众号留言，我们会尽快为您解答。";
    }
}
