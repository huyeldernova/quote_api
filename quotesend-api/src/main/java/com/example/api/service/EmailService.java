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

    @Value("${app.base-url}")     private String baseUrl;
    @Value("${spring.mail.username}") private String fromEmail;

    public void sendQuoteEmail(Quote quote, EmailLog emailLog, String message) {
        try {
            // 1 – Generate PDF bytes
            byte[] pdfBytes = pdfService.generateQuotePdf(quote);

            // 2 – Build HTML body via Thymeleaf
            DecimalFormat fmt = new DecimalFormat("#,##0");
            Context ctx = new Context();
            ctx.setVariable("tourName",     quote.getTourName());
            ctx.setVariable("startDate",    quote.getStartDate());
            ctx.setVariable("paxCount",     quote.getPaxCount());
            ctx.setVariable("pricePerPerson", fmt.format(quote.getPricePerPerson() != null ? quote.getPricePerPerson() : 0));
            ctx.setVariable("totalAmount",  fmt.format(quote.getTotalAmount() != null ? quote.getTotalAmount() : 0));
            ctx.setVariable("message",      message);
            ctx.setVariable("trackingUrl",
                    baseUrl + "/track/open/" + emailLog.getTrackingToken());
            String htmlBody = templateEngine.process("email-quote", ctx);

            // 3 – Build MimeMessage
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail, "Tourist Leader");
            helper.setTo(emailLog.getToEmail());
            if (emailLog.getCcEmail() != null && !emailLog.getCcEmail().isBlank()) {
                helper.setCc(emailLog.getCcEmail());
            }
            helper.setSubject(emailLog.getSubject());
            helper.setText(htmlBody, true);

            // 4 – Attach PDF
            String filename = "Quote_%s_%s.pdf".formatted(
                    quote.getQuoteNumber(), quote.getClientName().replaceAll("\\s+", "_"));
            helper.addAttachment(filename, () -> new java.io.ByteArrayInputStream(pdfBytes),
                    "application/pdf");

            mailSender.send(mime);
            log.info("Quote email sent to {} for quote {}", emailLog.getToEmail(), quote.getQuoteNumber());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
