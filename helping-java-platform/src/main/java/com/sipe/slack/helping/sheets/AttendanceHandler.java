package com.sipe.slack.helping.sheets;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.input;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.multiUsersSelect;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import static com.slack.api.model.view.Views.view;
import static com.slack.api.model.view.Views.viewClose;
import static com.slack.api.model.view.Views.viewSubmit;
import static com.slack.api.model.view.Views.viewTitle;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState.Value;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AttendanceHandler {

  public SlashCommandHandler attendance() {
    return (req, ctx) -> {
      ViewsOpenResponse viewsOpenRes = ctx.client().viewsOpen(r -> r
          .triggerId(ctx.getTriggerId())
          .view(buildView()));
      if (viewsOpenRes.isOk()) {
        return ctx.ack();
      } else {
        return Response.builder().statusCode(500).body(viewsOpenRes.getError()).build();
      }
    };
  }

  private View buildView() {
    return view(view -> view
        .callbackId("attendance")
        .type("modal")
        .notifyOnClose(true)
        .title(viewTitle(title -> title
            .type("plain_text")
            .text("출석체크")
            .emoji(true)
        ))
        .submit(viewSubmit(submit -> submit
            .type("plain_text")
            .text("제출")
            .emoji(true)
        ))
        .close(viewClose(close -> close
            .type("plain_text")
            .text("취소")
            .emoji(true)
        ))
        .blocks(asBlocks(
            // 주차 입력 블록
            input(input -> input
                .blockId("week-block")
                .element(plainTextInput(pti -> pti
                    .actionId("week-input-action")
                    .placeholder(plainText("주차를 입력하세요 (숫자)"))
                ))
                .label(plainText(pt -> pt
                    .text("주차 입력")
                    .emoji(true)
                ))
            ),
            // 멤버 선택 블록
            input(input -> input
                .blockId("attendance-block")
                .element(multiUsersSelect(usersSelect -> usersSelect
                    .actionId("attendance-select-action")
                    .placeholder(plainText("출석 멤버를 선택해주세요"))
                ))
                .label(plainText(pt -> pt
                    .text("출석한 멤버를 선택해주세요")
                    .emoji(true)
                ))
            )
        ))
    );
  }

  public ViewSubmissionHandler handleSubmission() {
    return (req, ctx) -> {
      try {
        // View Submission Payload에서 입력값 추출
        String memberBlockId = "attendance-block";
        String memberActionId = "attendance-select-action";

        // 선택된 유저 ID 목록 추출
        List<String> selectedUserIds = req.getPayload().getView().getState().getValues()
            .get(memberBlockId)
            .get(memberActionId)
            .getSelectedUsers();
        log.info("Selected Users: {}", selectedUserIds);

        // View Submission Payload에서 입력값 추출
        String weekBlockId = "week-block";
        String weekActionId = "week-input-action";

        String weekInput = req.getPayload().getView().getState().getValues()
            .get(weekBlockId)
            .get(weekActionId)
            .getValue();

        log.info("Input Week: {}", weekInput);

        // 추가 로직 (데이터 저장, 처리 등)
        // 예: DB 저장, 메시지 전송 등
        // processAttendance(selectedUserIds);

        // 사용자에게 성공 메시지 반환
        return ctx.ack(r -> r.responseAction("clear"));
      } catch (Exception e) {
        log.error("Failed to handle view submission", e);
        return Response.builder().statusCode(500).body("Internal Server Error").build();
      }
    };
  }

  private void processAttendance(List<String> userIds) {
    // 유저 데이터를 처리하는 로직 (예: DB 저장)
    log.info("Processing attendance for users: {}", userIds);
  }
}
