package com.example.api.service;

import com.example.api.entity.Quote;
import com.example.api.entity.QuoteCost;
import com.example.api.entity.QuoteDay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfService {

    public byte[] generateQuotePdf(Quote quote) {
        String xhtml = buildXhtml(quote);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private String buildXhtml(Quote q) {
        double totalCost = q.getCosts().stream()
                .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0).sum();

        double ppp     = q.getPricePerPerson() != null ? q.getPricePerPerson() : 0;
        double total   = q.getTotalAmount()    != null ? q.getTotalAmount()    : 0;
        double deposit = total / 2;

        String today = q.getCreatedAt() != null
                ? q.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
        sb.append("<style type=\"text/css\">");
        sb.append(CSS);
        sb.append("</style></head><body>");

        // ── Header ──────────────────────────────────────────────────────
        sb.append("<div class=\"header\">");
        sb.append("<div><div class=\"company\">TOURIST LEADER</div>");
        sb.append("<div class=\"tagline\">EXPLORE THE WORLD</div></div>");
        sb.append("<div class=\"header-contact\">");
        sb.append("<div>help@touristleader.com</div>");
        sb.append("<div>+91 080 6218-2211</div>");
        sb.append("<div class=\"gold\">www.touristleader.com</div>");
        sb.append("</div></div>");
        sb.append("<div class=\"gold-bar\">&#160;</div>");

        // ── Title block ──────────────────────────────────────────────────
        sb.append("<div class=\"title-block\">");
        sb.append("<div class=\"sub-label\">TRAVEL QUOTATION</div>");
        sb.append("<div class=\"tour-name\">").append(esc(nvl(q.getTourType(), "Travel Itinerary"))).append("</div>");
        sb.append("<div class=\"route\">").append(esc(nvl(q.getRouteFrom()))).append(" &#8212; ").append(esc(nvl(q.getRouteTo()))).append("</div>");
        sb.append("<div class=\"badge\">").append(q.getPaxCount()).append(" Passengers &#183; ").append(calcDuration(q.getDays().size())).append("</div>");
        sb.append("<div class=\"issue-date\">Issued: ").append(today).append("</div>");
        sb.append("</div>");

        // ── Salutation ───────────────────────────────────────────────────
        sb.append("<p class=\"salutation\">Dear <strong>").append(esc(nvl(q.getClientName(), "Sir/Madam"))).append("</strong>,</p>");
        sb.append("<p class=\"intro\">Greetings from <strong>Tourist Leader!</strong> Please find our exclusive travel quotation for the <strong>").append(esc(nvl(q.getTourName()))).append("</strong> package.</p>");

        // ── Trip Summary ─────────────────────────────────────────────────
        sb.append("<div class=\"section-title\">Trip Summary</div>");
        sb.append("<table class=\"info-table\"><tbody>");
        infoRow(sb, "Tour Name",      nvl(q.getTourName()));
        infoRow(sb, "Tour Type",      nvl(q.getTourType()));
        infoRow(sb, "Departure Date", nvl(q.getStartDate()));
        infoRow(sb, "Return Date",    nvl(q.getEndDate()));
        infoRow(sb, "Transportation", nvl(q.getTransport()));
        infoRow(sb, "Accommodation",  nvl(q.getStarRating()));
        sb.append("</tbody></table>");

        // ── Itinerary ─────────────────────────────────────────────────────
        if (!q.getDays().isEmpty()) {
            sb.append("<div class=\"section-title\">Itinerary</div>");
            sb.append("<table class=\"data-table\"><thead><tr>");
            sb.append("<th>Day</th><th>Date</th><th>Location</th><th>Hotel / Accommodation</th><th>Sightseeing / Activities</th>");
            sb.append("</tr></thead><tbody>");
            for (QuoteDay d : q.getDays()) {
                sb.append("<tr>");
                sb.append("<td style=\"vertical-align:top;\"><strong>Day ").append(d.getDayNumber()).append("</strong></td>");
                sb.append("<td style=\"vertical-align:top;\">").append(esc(nvl(d.getDateLabel()))).append("</td>");

                // Location + ảnh (nếu có)
                sb.append("<td style=\"vertical-align:top;\">");
                if (d.getImageUrl() != null && !d.getImageUrl().isBlank()) {
                    sb.append("<img src=\"").append(d.getImageUrl()).append("\" ")
                            .append("style=\"width:110px;height:70px;object-fit:cover;border-radius:3px;display:block;margin-bottom:4px;\"/>");
                }
                sb.append(esc(nvl(d.getLocation()))).append("</td>");

                sb.append("<td style=\"vertical-align:top;\">").append(esc(nvl(d.getHotel()))).append("</td>");
                sb.append("<td style=\"vertical-align:top;\">").append(esc(nvl(d.getSights()))).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        // ── Cost breakdown ────────────────────────────────────────────────
        if (!q.getCosts().isEmpty()) {
            sb.append("<div class=\"section-title\">Cost Breakdown</div>");
            sb.append("<table class=\"data-table\"><thead><tr><th>Item</th><th>Amount per Person (INR)</th></tr></thead><tbody>");
            for (QuoteCost c : q.getCosts()) {
                sb.append("<tr><td>").append(esc(nvl(c.getLabel()))).append("</td>")
                        .append("<td>").append(fmtInr(c.getAmount())).append("</td></tr>");
            }
            sb.append("<tr class=\"total-row\"><td><strong>Total Cost / Person</strong></td>")
                    .append("<td><strong>").append(fmtInr(totalCost)).append("</strong></td></tr>");
            sb.append("</tbody></table>");
        }

        // ── Pricing boxes ─────────────────────────────────────────────────
        sb.append("<div class=\"section-title\">Package Pricing</div>");
        sb.append("<table class=\"price-boxes\"><tbody><tr>");
        sb.append("<td class=\"price-box green\">");
        sb.append("<div class=\"pb-label\">Rate per Person</div>");
        sb.append("<div class=\"pb-value\">INR ").append(Math.round(ppp)).append(" /-</div>");
        sb.append("</td>");
        sb.append("<td class=\"price-box blue\">");
        sb.append("<div class=\"pb-label\">Group Total (").append(q.getPaxCount()).append(" pax)</div>");
        sb.append("<div class=\"pb-value\">INR ").append(Math.round(total)).append(" /-</div>");
        sb.append("<div class=\"pb-sub\">Deposit 50%: INR ").append(Math.round(deposit)).append(" /-</div>");
        sb.append("</td></tr></tbody></table>");

        // ── Inclusions / Exclusions ───────────────────────────────────────
        sb.append("<table class=\"incl-table\"><tbody><tr>");
        sb.append("<td class=\"incl-col\">");
        sb.append("<div class=\"section-title\" style=\"margin-top:0;\">Included</div>");
        for (String x : INCLUDED) {
            sb.append("<div class=\"incl-row\"><span class=\"badge-green\">+</span>&#160;").append(esc(x)).append("</div>");
        }
        sb.append("</td><td class=\"incl-col\">");
        sb.append("<div class=\"section-title\" style=\"margin-top:0;\">Not Included</div>");
        for (String x : EXCLUDED) {
            sb.append("<div class=\"incl-row\"><span class=\"badge-red\">-</span>&#160;").append(esc(x)).append("</div>");
        }
        sb.append("</td></tr></tbody></table>");

        // ── Terms ─────────────────────────────────────────────────────────
        sb.append("<div class=\"terms\">");
        sb.append("<strong>Terms &amp; Conditions:</strong> ");
        sb.append("Rates quoted are based on current availability and subject to change without prior notice. ");
        sb.append("Cancellation charges apply as per company policy. Tourist Leader shall not be liable for ");
        sb.append("delays or losses due to circumstances beyond our control.");
        sb.append("</div>");

        // ── Footer ────────────────────────────────────────────────────────
        sb.append("<div class=\"footer\">");
        sb.append("<div class=\"footer-inner\">");
        sb.append("<div><strong class=\"gold\">MYTL Adventures Private Limited</strong><br/>");
        sb.append("CIN: U52291KA2024PTC191143 | GST: 29AASCM3496M1ZD</div>");
        sb.append("<div style=\"text-align:right;\">help@touristleader.com<br/>");
        sb.append("+91 080 6218-2211<br/>");
        sb.append("<span class=\"gold\">www.touristleader.com</span></div>");
        sb.append("</div>");
        sb.append("<div class=\"footer-address\">112/5 Dr Ambedkar Road, Whitefield, Bangalore 560066, Karnataka, India</div>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    // ─── helpers ─────────────────────────────────────────────────────────
    private void infoRow(StringBuilder sb, String label, String value) {
        sb.append("<tr><td class=\"info-label\">").append(esc(label)).append("</td>")
                .append("<td>").append(esc(value)).append("</td></tr>");
    }

    private String nvl(String s, String def) { return (s != null && !s.isBlank()) ? s : def; }
    private String nvl(String s)             { return nvl(s, "—"); }
    private String fmtInr(Double v)          { return String.format("INR %,.0f /-", v != null ? v : 0); }
    private String calcDuration(int days)    { return Math.max(days - 1, 0) + "N / " + days + "D"; }

    private String esc(String s) {
        if (s == null) return "—";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    // ─── CSS ─────────────────────────────────────────────────────────────
    private static final String CSS = """
        body { font-family: Arial, Helvetica, sans-serif; font-size: 11px; color: #1A1A2E; margin: 20px; }
        .header { background-color: #0F2050; color: white; padding: 16px 24px;
                  display: table; width: 100%; }
        .company { font-size: 20px; font-weight: bold; letter-spacing: 3px; color: white; }
        .tagline { color: #C9A84C; font-size: 9px; letter-spacing: 3px; }
        .header-contact { text-align: right; font-size: 10px; color: rgba(255,255,255,0.85); }
        .gold { color: #C9A84C; }
        .gold-bar { background-color: #C9A84C; height: 3px; margin: 0; }
        .title-block { background-color: #F9FAFB; border-bottom: 1px solid #D1D5DB;
                       padding: 14px 24px; margin-bottom: 14px; }
        .sub-label { font-size: 9px; color: #6B7280; letter-spacing: 3px; text-transform: uppercase; }
        .tour-name { font-size: 18px; font-weight: bold; color: #0F2050; margin-top: 4px; }
        .route { font-size: 12px; color: #374151; }
        .badge { display: inline-block; background-color: #1E3A6E; color: white;
                 padding: 3px 10px; border-radius: 3px; font-size: 10px; margin-top: 6px; }
        .issue-date { font-size: 10px; color: #6B7280; margin-top: 4px; }
        .salutation { font-size: 12px; color: #374151; line-height: 1.6; margin: 8px 0 4px; }
        .intro { font-size: 11px; color: #374151; line-height: 1.7; margin-bottom: 14px; }
        .section-title { border-left: 4px solid #C9A84C; padding-left: 8px; font-size: 12px;
                         font-weight: bold; text-transform: uppercase; color: #0F2050;
                         margin: 14px 0 8px; }
        .info-table { width: 100%; border-collapse: collapse; font-size: 11px; margin-bottom: 12px; }
        .info-table td { padding: 6px 10px; border-bottom: 1px solid #E5E7EB; }
        .info-label { color: #6B7280; width: 160px; }
        .data-table { width: 100%; border-collapse: collapse; font-size: 11px; margin-bottom: 12px; }
        .data-table th { background-color: #1E3A6E; color: white; padding: 7px 10px; text-align: left; }
        .data-table td { padding: 6px 10px; border-bottom: 1px solid #E5E7EB; vertical-align: top; }
        .data-table tr:nth-child(even) td { background-color: #F9FAFB; }
        .total-row td { background-color: #EEF3FB !important; font-weight: bold; }
        .price-boxes { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
        .price-box { width: 50%; padding: 12px 14px; vertical-align: top; }
        .price-box.green { background-color: #F0FDF4; border: 1px solid #86EFAC;
                           border-top: 3px solid #16A34A; }
        .price-box.blue  { background-color: #EFF6FF; border: 1px solid #93C5FD;
                           border-top: 3px solid #2563EB; }
        .pb-label { font-size: 9px; font-weight: bold; text-transform: uppercase;
                    letter-spacing: 1px; margin-bottom: 4px; }
        .price-box.green .pb-label { color: #166534; }
        .price-box.blue  .pb-label { color: #1D4ED8; }
        .pb-value { font-size: 18px; font-weight: bold; }
        .price-box.green .pb-value { color: #15803D; }
        .price-box.blue  .pb-value { color: #1D4ED8; }
        .pb-sub { font-size: 10px; margin-top: 3px; color: #1D4ED8; }
        .incl-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
        .incl-col { width: 50%; padding: 0 8px 0 0; vertical-align: top; }
        .incl-row { display: block; margin-bottom: 5px; font-size: 11px; color: #374151; }
        .badge-green { background-color: #16A34A; color: white; border-radius: 8px;
                       padding: 0 4px; font-size: 9px; font-weight: bold; }
        .badge-red   { background-color: #DC2626; color: white; border-radius: 8px;
                       padding: 0 4px; font-size: 9px; font-weight: bold; }
        .terms { font-size: 10px; color: #6B7280; border-top: 1px solid #E5E7EB;
                 padding-top: 10px; margin-top: 10px; line-height: 1.7; }
        .footer { background-color: #0F2050; color: rgba(255,255,255,0.8);
                  padding: 12px 24px; margin-top: 14px; border-top: 3px solid #C9A84C; }
        .footer-inner { display: table; width: 100%; font-size: 10px; }
        .footer-address { font-size: 9px; color: rgba(255,255,255,0.3);
                          text-align: center; margin-top: 6px; }
        """;

    private static final String[] INCLUDED = {
            "Accommodation in triple sharing room with daily breakfast and dinner",
            "All transfers and transportation with air-conditioned vehicles",
            "01 bottle mineral water per person per day (transfer days only)",
            "Experienced tour guide throughout the journey"
    };
    private static final String[] EXCLUDED = {
            "Personal expenses and laundry charges",
            "Early check-in or late check-out fees",
            "Entrance fees to monuments and attractions",
            "Optional excursions not mentioned in itinerary",
            "Meals other than those mentioned",
            "Personal travel insurance"
    };
}