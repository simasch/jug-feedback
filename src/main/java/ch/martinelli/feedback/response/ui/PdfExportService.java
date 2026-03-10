package ch.martinelli.feedback.response.ui;

import ch.martinelli.feedback.form.domain.FeedbackForm;
import ch.martinelli.feedback.form.domain.FormService;
import ch.martinelli.feedback.form.domain.QuestionType;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PdfExportService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Color BAR_COLOR = new Color(0, 120, 215);
    private static final Color BAR_BG_COLOR = new Color(230, 230, 230);

    public byte[] generateResultsPdf(FeedbackForm form, FormService formService) {
        var baos = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, baos);
        document.open();

        document.add(new Paragraph(form.title(), TITLE_FONT));

        if (form.speakerName() != null && !form.speakerName().isEmpty()) {
            document.add(new Paragraph("Speaker: " + form.speakerName(), NORMAL_FONT));
        }

        document.add(new Paragraph("Export date: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), SMALL_FONT));
        document.add(Chunk.NEWLINE);

        var responseCount = formService.getResponseCount(form.id());
        document.add(new Paragraph("Total responses: " + responseCount, BOLD_FONT));
        document.add(Chunk.NEWLINE);

        if (responseCount == 0) {
            document.add(new Paragraph("No responses yet.", NORMAL_FONT));
            document.close();
            return baos.toByteArray();
        }

        for (var question : form.questions()) {
            document.add(new Paragraph(question.orderIndex() + ". " + question.questionText(), HEADING_FONT));
            document.add(Chunk.NEWLINE);

            if (question.questionType() == QuestionType.RATING) {
                addRatingDistribution(document, question.id(), formService);
            } else {
                addTextAnswers(document, question.id(), formService);
            }

            document.add(Chunk.NEWLINE);
        }

        document.close();
        return baos.toByteArray();
    }

    private void addRatingDistribution(Document document, Long questionId, FormService formService) {
        var distribution = formService.getRatingDistribution(questionId);
        long totalRatings = distribution.values().stream().mapToLong(Long::longValue).sum();
        long maxCount = distribution.values().stream().mapToLong(Long::longValue).max().orElse(1);

        var table = new PdfPTable(3);
        table.setWidthPercentage(80);
        table.setWidths(new float[]{8, 60, 32});

        for (Map.Entry<Integer, Long> entry : distribution.entrySet()) {
            int rating = entry.getKey();
            long count = entry.getValue();
            double percentage = totalRatings > 0 ? (count * 100.0) / totalRatings : 0;
            float barWidthPct = maxCount > 0 ? (float) (count * 100.0 / maxCount) : 0;

            var labelCell = new PdfPCell(new Phrase(String.valueOf(rating), BOLD_FONT));
            labelCell.setBorder(0);
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            labelCell.setPaddingRight(5);
            table.addCell(labelCell);

            var barCell = new PdfPCell();
            barCell.setBorder(0);
            barCell.setCellEvent((cell, position, canvases) -> {
                var canvas = canvases[com.lowagie.text.pdf.PdfPTable.BACKGROUNDCANVAS];
                float x = position.getLeft();
                float y = position.getBottom();
                float w = position.getWidth();
                float h = position.getHeight() - 4;
                float yOffset = y + 2;

                canvas.setColorFill(BAR_BG_COLOR);
                canvas.rectangle(x, yOffset, w, h);
                canvas.fill();

                if (barWidthPct > 0) {
                    canvas.setColorFill(BAR_COLOR);
                    canvas.rectangle(x, yOffset, w * barWidthPct / 100f, h);
                    canvas.fill();
                }
            });
            barCell.setFixedHeight(18);
            barCell.setPhrase(new Phrase(" "));
            table.addCell(barCell);

            var countCell = new PdfPCell(new Phrase(String.format("  %d (%.0f%%)", count, percentage), SMALL_FONT));
            countCell.setBorder(0);
            countCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(countCell);
        }

        document.add(table);

        var avg = formService.getAverageRating(questionId);
        if (avg != null) {
            document.add(new Paragraph(
                    String.format("Average rating: %.2f / 5 (%d ratings)", avg, totalRatings), BOLD_FONT));
        } else {
            document.add(new Paragraph("Average rating: N/A", NORMAL_FONT));
        }
    }

    private void addTextAnswers(Document document, Long questionId, FormService formService) {
        var textAnswers = formService.getTextAnswers(questionId);
        var list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
        list.setListSymbol("\u2022  ");
        for (var answer : textAnswers) {
            if (answer.textValue() != null && !answer.textValue().trim().isEmpty()) {
                list.add(new ListItem(new Phrase(answer.textValue(), NORMAL_FONT)));
            }
        }
        document.add(list);
    }
}
