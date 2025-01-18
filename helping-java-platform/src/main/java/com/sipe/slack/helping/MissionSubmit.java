package com.sipe.slack.helping;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import com.slack.api.model.block.composition.OptionObject;

import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.element.StaticSelectElement;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import com.slack.api.model.view.Views;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MissionSubmit {

    private static final String TITLE_INPUT = "title_input";
    private static final String SENTENCE_INPUT = "sentence_input";
    private static final String COMMENT_INPUT = "comment_input";
    private static final String SUBMIT_VIEW = "submit_view";

    public SlashCommandHandler handleSubmitCommand() {
        return (req, ctx) -> {
            try {
                ViewsOpenResponse response = ctx.client().viewsOpen(r -> r
                        .triggerId(req.getPayload().getTriggerId())
                        .view(buildSubmitView(req.getPayload().getChannelId()))
                );
                if (!response.isOk()) {
                    log.error("Error opening view: {}", response.getError());
                }
            } catch (IOException | SlackApiException e) {
                log.error("Error handling submit command", e);
            }
            return ctx.ack();
        };
    }

    // req: Request(요청), ctx: Context(컨텍스트)
    public SlashCommandHandler testHandler() {
        return (req, ctx) -> {
            try {
                log.info("test Handler");
                ctx.client().chatPostMessage(r -> r.channel(req.getPayload().getChannelId()).text("test"));
            } catch (SlackApiException e) {
                log.error("Error handling test command", e);
            }
            return ctx.ack();
        };
    }

    public ViewSubmissionHandler handleViewSubmission() {
        return (req, ctx) -> {
            try {
                String channelId = req.getPayload().getView().getPrivateMetadata();
                if (!channelId.equals("C073SJEJ4GL")) {
                    ctx.ack(r -> r.responseAction("errors").errors(Map.of("sentence_block_id", "#오늘의 문장 채널에서만 제출할 수 있습니다.")));
                    return ctx.ack();
                }

                ViewState.Value sentenceValue = req.getPayload().getView().getState().getValues().get("sentence_block_id").get(SENTENCE_INPUT);
                if (sentenceValue.getValue().length() < 3) {
                    ctx.ack(r -> r.responseAction("errors").errors(Map.of("sentence_block_id", "오늘의 문장은 세 글자 이상 입력해주세요.")));
                    return ctx.ack();
                }

                ctx.ack();

                String userId = req.getPayload().getUser().getId();
                String userName = ctx.client().usersInfo(r -> r.user(userId)).getUser().getRealName();
                String bookTitle = req.getPayload().getView().getState().getValues().get("title_block_id").get(TITLE_INPUT).getValue();
                String sentence = sentenceValue.getValue();
                String comment = req.getPayload().getView().getState().getValues().get("comment_block_id").get(COMMENT_INPUT).getValue();
                String createdAt = new Date().toString();

                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdir();
                }

                File csvFile = new File("data/contents.csv");
                if (!csvFile.exists()) {
                    try (FileWriter writer = new FileWriter(csvFile)) {
                        writer.write("user_id,user_name,book_title,sentence,comment,created_at\n");
                    }
                }

                String newRow = String.format("%s,%s,%s,%s,%s,%s\n", userId, userName, bookTitle, sentence, comment, createdAt);
                Files.write(Paths.get("data/contents.csv"), newRow.getBytes(), java.nio.file.StandardOpenOption.APPEND);

                String text = String.format(">>> *<@%s>님이 `%s` 에서 뽑은 오늘의 문장*\n\n '%s'\n", userId, bookTitle, sentence);
                if (comment != null && !comment.isEmpty()) {
                    text += String.format("\n %s\n", comment);
                }

                String finalText = text;
                ctx.client().chatPostMessage(r -> r.channel(channelId).text(finalText));
            } catch (IOException | SlackApiException e) {
                log.error("Error handling view submission", e);
            }
            return ctx.ack();
        };
    }

    public SlashCommandHandler handleSubmissionHistoryCommand() {
        return (req, ctx) -> {
            try {
                String userId = req.getPayload().getUserId();
                String dmChannelId = ctx.client().conversationsOpen(r -> r.users(List.of(userId))).getChannel().getId();

                File csvFile = new File("data/contents.csv");
                if (!csvFile.exists()) {
                    ctx.client().chatPostMessage(r -> r.channel(dmChannelId).text("제출내역이 없습니다."));
                    return ctx.ack();
                }

                List<Map<String, String>> submissionList = Files.readAllLines(Paths.get("data/contents.csv")).stream()
                        .skip(1)
                        .map(line -> line.split(","))
                        .filter(fields -> fields[0].equals(userId))
                        .map(fields -> Map.of(
                                "user_id", fields[0],
                                "user_name", fields[1],
                                "book_title", fields[2],
                                "sentence", fields[3],
                                "comment", fields[4],
                                "created_at", fields[5]
                        ))
                        .toList();

                if (submissionList.isEmpty()) {
                    ctx.client().chatPostMessage(r -> r.channel(dmChannelId).text("제출내역이 없습니다."));
                    return ctx.ack();
                }

                File tempDir = new File("data/temp");
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }

                File tempFile = new File(tempDir, userId + ".csv");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write("user_id,user_name,book_title,sentence,comment,created_at\n");
                    for (Map<String, String> submission : submissionList) {
                        writer.write(String.join(",", submission.values()) + "\n");
                    }
                }

                ctx.client().filesUpload(r -> r
                        .channels(List.of(dmChannelId))
                        .file(tempFile)
                        .initialComment(String.format("<@%s> 님의 제출내역 입니다!", userId))
                );

                tempFile.delete();
            } catch (IOException | SlackApiException e) {
                log.error("Error handling submission history command", e);
            }
            return ctx.ack();
        };
    }

    public SlashCommandHandler handleAdminCommand() {
        return (req, ctx) -> {
            try {
                String userId = req.getPayload().getUserId();
                if (!userId.equals("U073M3MVA13")) {
                    ctx.client().chatPostEphemeral(r -> r
                            .channel(req.getPayload().getChannelId())
                            .user(userId)
                            .text("관리자만 사용 가능한 명령어입니다.")
                    );
                    return ctx.ack();
                }

                ctx.client().chatPostEphemeral(r -> r
                        .channel(req.getPayload().getChannelId())
                        .user(userId)
                        .text("관리자 메뉴를 선택해주세요.")
                        .blocks(asBlocks(
                                Blocks.actions(actions -> actions
                                        .elements(asElements(
                                                BlockElements.button(button -> button
                                                        .text(plainText(pt -> pt.text("전체 제출내역 조회").emoji(true)))
                                                        .value("admin_value_1")
                                                        .actionId("fetch_all_submissions")
                                                )
                                        ))
                                )
                        ))
                );
            } catch (SlackApiException | IOException e) {
                log.error("Error handling admin command", e);
            }
            return ctx.ack();
        };
    }

    public BlockActionHandler handleFetchAllSubmissions() {
        return (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                String dmChannelId = ctx.client().conversationsOpen(r -> r.users(List.of(userId))).getChannel().getId();

                File csvFile = new File("data/contents.csv");
                if (!csvFile.exists()) {
                    ctx.client().chatPostMessage(r -> r.channel(dmChannelId).text("제출내역이 없습니다."));
                    return ctx.ack();
                }

                ctx.client().filesUpload(r -> r
                        .channels(List.of(dmChannelId))
                        .file(csvFile)
                        .initialComment("전체 제출내역 입니다!")
                );
            } catch (IOException | SlackApiException e) {
                log.error("Error handling fetch all submissions action", e);
            }
            return ctx.ack();
        };
    }

    private View buildSubmitView(String channelId) {
        return Views.view(view -> view
                .callbackId(SUBMIT_VIEW)
                .type("modal")
                .privateMetadata(channelId)
                // .title(viewTitle -> viewTitle.type("plain_text").text("제출하기"))
                // .submit(viewSubmit -> viewSubmit.type("plain_text").text("제출"))
                // .close(viewClose -> viewClose.type("plain_text").text("취소"))
                .blocks(asBlocks(
                        Blocks.input(input -> input
                                .blockId("title_block_id")
                                .label(plainText(pt -> pt.text("책 제목")))
                                .element(plainTextInput(pti -> pti.actionId(TITLE_INPUT).placeholder(plainText(pt -> pt.text("책 제목을 입력해주세요.")))))
                        ),
                        Blocks.input(input -> input
                                .blockId("sentence_block_id")
                                .label(plainText(pt -> pt.text("오늘의 문장")))
                                .element(plainTextInput(pti -> pti.actionId(SENTENCE_INPUT).multiline(true).placeholder(plainText(pt -> pt.text("기억에 남는 문장을 입력해주세요.")))))
                        ),
                        Blocks.input(input -> input
                                .blockId("comment_block_id")
                                .label(plainText(pt -> pt.text("생각 남기기")))
                                .optional(true)
                                .element(plainTextInput(pti -> pti.actionId(COMMENT_INPUT).multiline(true).placeholder(plainText(pt -> pt.text("생각을 자유롭게 남겨주세요.")))))
                        )
                ))
        );
    }
}