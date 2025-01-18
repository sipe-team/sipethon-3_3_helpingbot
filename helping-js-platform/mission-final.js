import fs from 'fs';
import dotenv from 'dotenv';
import { GoogleSheet } from './googlesheet.js';

dotenv.config();

export const handleMissionFinalSelect = async ({ command, ack, client }) => {
  await ack();

  try {
    const result = await client.views.open({
      trigger_id: command.trigger_id,
      view: {
        type: 'modal',
        callback_id: 'mission_apply_submission',
        title: {
          type: 'plain_text',
          text: '미션 선발',
          emoji: true,
        },
        blocks: [
          {
            type: 'actions',
            block_id: 'send_result',
            elements: [
              {
                type: 'button',
                text: {
                  type: 'plain_text',
                  text: '팀 배정 결과 알림',
                  emoji: true,
                },
                style: 'primary',
                action_id: 'send_result',
              },
            ],
          },
        ],
      },
    });
  } catch (error) {
    console.error('미션 신청 모달 열기 중 에러:', error);
  }
};

export const handleSendResult = async ({ body, ack, client }) => {
  if (ack) await ack();

  try {
    const googlesheet = new GoogleSheet();
    await googlesheet.init();
    const records = await googlesheet.readMission();

    // 메시지 블록 생성
    const messageBlocks = [
      {
        type: 'header',
        text: {
          type: 'plain_text',
          text: '🎉 미션 최종 배정 결과',
          emoji: true,
        },
      },
      {
        type: 'divider',
      },
    ];

    // 각 미션 데이터를 섹션 블록으로 변환
    records.forEach((row) => {
      const [_, teamName, subject, goal, rule, plan, __, ___, ____, final] =
        row;

      // 빈 데이터는 건너뛰기
      if (!teamName && !subject && !final) return;

      messageBlocks.push(
        {
          type: 'section',
          fields: [
            {
              type: 'mrkdwn',
              text: `*팀명:*\n${teamName || '-'}`,
            },
            {
              type: 'mrkdwn',
              text: `*미션명:*\n${subject || '-'}`,
            },
            {
              type: 'mrkdwn',
              text: `*최종 배정:*\n${final || '-'}`,
            },
          ],
        },
        {
          type: 'divider',
        }
      );
    });

    // 마지막 업데이트 시간 추가
    messageBlocks.push({
      type: 'context',
      elements: [
        {
          type: 'mrkdwn',
          text: `마지막 업데이트: ${new Date().toLocaleString('ko-KR')}`,
        },
      ],
    });

    // 메시지 전송
    await client.chat.postMessage({
      channel: 'C0893D5CG6N',
      blocks: messageBlocks,
      text: '미션 최종 배정 결과입니다.', // 알림이 꺼져있는 경우를 위한 폴백 텍스트
    });
  } catch (error) {
    console.error('미션 결과 전송 중 에러:', error);

    // 에러 메시지 전송
    await client.chat.postMessage({
      channel: 'C0893D5CG6N',
      text: '❌ 미션 배정 결과 전송 중 오류가 발생했습니다.',
    });
  }
};
