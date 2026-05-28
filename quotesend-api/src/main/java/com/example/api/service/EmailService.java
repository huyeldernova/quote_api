package com.example.api.service;

import com.example.api.entity.EmailLog;
import com.example.api.entity.Quote;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.DecimalFormat;

@Service @RequiredArgsConstructor @Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final PdfService     pdfService;

    @Value("${app.base-url}")         private String baseUrl;
    @Value("${spring.mail.username}") private String fromEmail;

    public void sendQuoteEmail(Quote quote, EmailLog emailLog, String message) {
        try {
            byte[] pdfBytes = pdfService.generateQuotePdf(quote);

            DecimalFormat fmt = new DecimalFormat("#,##0");
            Context ctx = new Context();
            ctx.setVariable("tourName",       quote.getTourName());
            ctx.setVariable("startDate",      quote.getStartDate());
            ctx.setVariable("paxCount",       quote.getPaxCount());
            ctx.setVariable("pricePerPerson", fmt.format(quote.getPricePerPerson() != null ? quote.getPricePerPerson() : 0));
            ctx.setVariable("totalAmount",    fmt.format(quote.getTotalAmount()    != null ? quote.getTotalAmount()    : 0));
            ctx.setVariable("message",        message);
            ctx.setVariable("trackingUrl",    baseUrl + "/track/open/" + emailLog.getTrackingToken());
            String htmlBody = templateEngine.process("email-quote", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail, "Tourist Leader");
            helper.setTo(emailLog.getToEmail());
            if (emailLog.getCcEmail() != null && !emailLog.getCcEmail().isBlank()) {
                helper.setCc(emailLog.getCcEmail());
            }
            helper.setSubject(emailLog.getSubject());
            helper.setText(htmlBody, true);

            String filename = "Quote_%s_%s.pdf".formatted(
                    quote.getQuoteNumber(),
                    quote.getClientName().replaceAll("\\s+", "_"));
            helper.addAttachment(filename,
                    () -> new java.io.ByteArrayInputStream(pdfBytes),
                    "application/pdf");

            mailSender.send(mime);
            log.info("Quote email sent to {} for quote {}", emailLog.getToEmail(), quote.getQuoteNumber());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    void sendWelcomeEmail(String email, String name) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail, "Tourist Leader");
            helper.setTo(email);
            helper.setSubject("Welcome to Tourist Leader - QuoteSend");
            helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background:#0F2050;padding:30px;text-align:center;">
                        <h1 style="color:white;margin:0;">TOURIST LEADER</h1>
                        <div style="color:#C9A84C;font-size:13px;margin-top:5px;">EXPLORE THE WORLD</div>
                    </div>
                    <div style="padding:40px;background:#fff;">
                        <h2 style="color:#0F2050;">Welcome, %s! 👋</h2>
                        <p style="color:#444;line-height:1.6;">
                            Your QuoteSend account has been created successfully.
                            You can now start creating professional travel quotations for your clients.
                        </p>
                        <div style="background:#EEF3FB;border-left:4px solid #1E3A6E;
                                    padding:15px 20px;margin:25px 0;">
                            <p style="margin:0;color:#1E3A6E;font-weight:bold;">
                                🚀 Get started in 3 steps:
                            </p>
                            <ol style="color:#444;margin:10px 0 0 0;padding-left:20px;">
                                <li>Create your first quote</li>
                                <li>Add tour details and costs</li>
                                <li>Send it to your client with one click</li>
                            </ol>
                        </div>
                        <a href="https://thedigitaldyno.in"
                           style="display:inline-block;background:#0F2050;color:white;
                                  padding:12px 30px;border-radius:6px;text-decoration:none;
                                  font-weight:bold;margin-top:10px;">
                            Go to QuoteSend →
                        </a>
                    </div>
                    <div style="background:#0F2050;padding:15px;text-align:center;
                                color:rgba(255,255,255,0.6);font-size:11px;">
                        help@touristleader.com | www.touristleader.com
                    </div>
                </div>
                """.formatted(name), true);
            mailSender.send(mime);
            log.info("Welcome email sent to {}", email);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", email, e.getMessage());
        }
    }
}