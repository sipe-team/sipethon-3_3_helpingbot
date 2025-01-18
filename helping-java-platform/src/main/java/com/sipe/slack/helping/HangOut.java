package com.sipe.slack.helping;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.Blocks;
import static com.slack.api.model.block.Blocks.asBlocks;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.OptionObject;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import com.slack.api.model.block.element.StaticSelectElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import com.slack.api.model.view.Views;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
public class HangOut {

    public SlashCommandHandler HangoutHandler() {
        return (req, ctx) -> {
            try {
                log.info("Hangout handler triggered.");

                // 모달창 설정
                ViewsOpenResponse response = ctx.client().viewsOpen(r -> r
                        .triggerId(req.getPayload().getTriggerId())
                        .view(buildHangoutView())
                );

                if (!response.isOk()) {
                    log.error("Error opening Hangout modal: {}", response.getError());
                } else {
                    log.info("Modal successfully opened: {}", response);
                }
            } catch (IOException | SlackApiException e) {
                log.error("Error handling Hangout command", e);
            }

            return ctx.ack();
        };
    }

    private View buildHangoutView() {
        return Views.view(view -> view
                .callbackId("hangout_view")
                .type("modal")
                .title(ViewTitle.builder().text("뒷풀이 참석 확인").type("plain_text").build()) // 제목
                .submit(ViewSubmit.builder().text("완료").type("plain_text").build())      // 완료 버튼
                .blocks(asBlocks(
                        Blocks.input(input -> input
                                .blockId("hangout_name_block") // 이름 입력 필드
                                .label(BlockCompositions.plainText("이름"))
                                .element(plainTextInput(pti -> pti
                                        .actionId("hangout_name_input")
                                        .placeholder(BlockCompositions.plainText("이름을 입력해주세요."))
                                ))
                        ),
                        Blocks.input(input -> input
                                .blockId("hangout_attendance_block") // 참석 여부 필드
                                .label(BlockCompositions.plainText("뒷풀이 참석 여부"))
                                .element(StaticSelectElement.builder()
                                        .actionId("hangout_attendance_input")
                                        .placeholder(BlockCompositions.plainText("참석 여부를 선택해주세요."))
                                        .options(Arrays.asList(
                                                createOption("참석", "yes"),
                                                createOption("불참", "no")
                                        ))
                                        .build()
                                )
                        )
                ))
        );
    }

    private OptionObject createOption(String text, String value) {
        return OptionObject.builder()
                .text(BlockCompositions.plainText(text))
                .value(value)
                .build();
    }

    public ViewSubmissionHandler handleHangoutSubmission() {
        return (req, ctx) -> {
            // 즉시 응답
            ctx.ack();

            // 데이터 처리
            try {
                String name = req.getPayload().getView().getState().getValues()
                        .get("hangout_name_block").get("hangout_name_input").getValue();
                String attendance = req.getPayload().getView().getState().getValues()
                        .get("hangout_attendance_block").get("hangout_attendance_input").getSelectedOption().getValue();

                String responseMessage = String.format("*뒷풀이 참석 정보*\n- 이름: %s\n- 참석 여부: %s",
                        name, attendance.equals("yes") ? "참석" : "불참");

                ctx.client().chatPostMessage(r -> r
                        .channel(req.getPayload().getUser().getId())
                        .text(responseMessage)
                );
            } catch (Exception e) {
                log.error("Error handling submission: {}", e.getMessage(), e);
            }

            return ctx.ack();
        };
    }
}
