package com.sipe.slack.helping;

import com.sipe.slack.helping.sheets.HangOutService;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
public class HangOut {
    private final HangOutService hangOutService;

    public HangOut(HangOutService hangOutService) {
        this.hangOutService = hangOutService;
    }

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

        String todayActivity = getTodayActivityMessage(); // 오늘 활동 메시지 가져오기
        return Views.view(view -> view
                .callbackId("hangout_view")
                .type("modal")
                .title(ViewTitle.builder().text("뒷풀이 참석 확인").type("plain_text").build()) // 제목
                .submit(ViewSubmit.builder().text("완료").type("plain_text").build())      // 완료 버튼
                .blocks(asBlocks(
                        // 회차 입력 필드
                        Blocks.section(selection -> selection
                                .blockId("hangout_activity_block")
                                .text(BlockCompositions.plainText("오늘의 활동: " + todayActivity))
                        ),
                        // 이름 입력 필드
                        Blocks.input(input -> input
                                .blockId("hangout_name_block")
                                .label(BlockCompositions.plainText("이름"))
                                .element(plainTextInput(pti -> pti
                                        .actionId("hangout_name_input")
                                        .placeholder(BlockCompositions.plainText("이름을 입력해주세요."))
                                ))
                        ),
                        // 참석 여부 필드
                        Blocks.input(input -> input
                                .blockId("hangout_attendance_block")
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

            try {
                // 데이터 가져오기
                String todayActivity = getTodayActivityMessage();
                String name = req.getPayload().getView().getState().getValues()
                        .get("hangout_name_block").get("hangout_name_input").getValue();
                String attendance = req.getPayload().getView().getState().getValues()
                        .get("hangout_attendance_block").get("hangout_attendance_input").getSelectedOption().getValue();

                // Google Sheets 저장
                try {
                    hangOutService.saveHangoutAttendance(todayActivity, name, attendance.equals("yes") ? "참석" : "불참");
                    ctx.client().chatPostMessage(r -> r
                            .channel(req.getPayload().getUser().getId())
                            .text(String.format(
                                    "*뒷풀이 참석 정보*\n" +
                                            "- 활동: %s\n" +
                                            "- 이름: %s\n" +
                                            "- 참석 여부: %s",
                                    todayActivity, name, attendance.equals("yes") ? "참석" : "불참"
                            ))
                    );
                } catch (RuntimeException e) {
                    ctx.client().chatPostMessage(r -> r
                            .channel(req.getPayload().getUser().getId())
                            .text("뒷풀이 참석 정보를 저장하는 데 실패했습니다. 관리자에게 문의해주세요.")
                    );
                }

            } catch (Exception e) {
                log.error("Error handling submission: {}", e.getMessage(), e);
            }

            return ctx.ack();
        };
    }

    private static final Map<LocalDate, String> WEEKLY_ACTIVITIES = Map.of(
            LocalDate.of(2024, 10, 12), "OT",
            LocalDate.of(2024, 10, 26), "MT",
            LocalDate.of(2024, 11, 9), "사이프챗",
            LocalDate.of(2024, 11, 23), "사이데이션",
            LocalDate.of(2024, 12, 7), "1차 미션 발표",
            LocalDate.of(2024, 12, 21), "사담콘",
            LocalDate.of(2025, 1, 4), "내친소",
            LocalDate.of(2025, 1, 18), "사이프톤",
            LocalDate.of(2025, 2, 1), "2차 미션 발표"
    );

    private String getTodayActivityMessage() {
        LocalDate today = LocalDate.now(); // 오늘 날짜 가져오기
        String activity = WEEKLY_ACTIVITIES.getOrDefault(today, "활동 없음");

        if ("활동 없음".equals(activity)) {
            return "오늘은 정규 활동이 없습니다.";
        } else {
            return String.format("%s 날 (%s)입니다.", activity, today);
        }
    }
}
