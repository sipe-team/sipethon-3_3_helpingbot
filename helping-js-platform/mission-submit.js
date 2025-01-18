import fs from 'fs';
import { MISSION_SUBMIT_MODAL } from './const.js';
import dotenv from 'dotenv';
import { GoogleSheet } from './googlesheet.js';

dotenv.config();

const NAME_INPUT = 'name_input';
const SUBJECT_INPUT = 'subject_input';
const GOAL_INPUT = 'goal_input';
const RULE_INPUT = 'rule_input';
const PLAN_INPUT = 'plan_input';

export const handleSubmitMission = async ({ body, client }) => {
  try {
    await client.views.open({
      trigger_id: body.trigger_id,
      view: {
        type: 'modal',
        callback_id: MISSION_SUBMIT_MODAL,
        title: {
          type: 'plain_text',
          text: '미션 발제',
          emoji: true,
        },
        submit: {
          type: 'plain_text',
          text: '제출',
          emoji: true,
        },
        close: {
          type: 'plain_text',
          text: '취소',
          emoji: true,
        },
        blocks: [
          {
            type: 'input',
            block_id: NAME_INPUT,
            optional: true,
            element: {
              type: 'plain_text_input',
              action_id: NAME_INPUT,
            },
            label: {
              type: 'plain_text',
              text: '팀 이름',
              emoji: true,
            },
          },
          {
            type: 'input',
            block_id: SUBJECT_INPUT,
            element: {
              type: 'plain_text_input',
              action_id: SUBJECT_INPUT,
              multiline: false,
              placeholder: {
                type: 'plain_text',
                text: '주제를 적어주세요.',
              },
            },
            label: {
              type: 'plain_text',
              text: '주제',
              emoji: true,
            },
          },
          {
            type: 'input',
            block_id: GOAL_INPUT,
            element: {
              type: 'plain_text_input',
              action_id: GOAL_INPUT,
              multiline: true,
              placeholder: {
                type: 'plain_text',
                text: '목표를 적어주세요.',
              },
            },
            label: {
              type: 'plain_text',
              text: '목표',
              emoji: true,
            },
          },
          {
            type: 'input',
            block_id: RULE_INPUT,
            optional: true,
            element: {
              type: 'plain_text_input',
              action_id: RULE_INPUT,
              multiline: true,
              placeholder: {
                type: 'plain_text',
                text: '규칙을 적어주세요.',
              },
            },
            label: {
              type: 'plain_text',
              text: '규칙',
              emoji: true,
            },
          },
          {
            type: 'input',
            block_id: PLAN_INPUT,
            element: {
              type: 'plain_text_input',
              action_id: PLAN_INPUT,
              multiline: true,
              placeholder: {
                type: 'plain_text',
                text: '계획을 적어주세요.',
              },
            },
            label: {
              type: 'plain_text',
              text: '주차별 활동 계획',
              emoji: true,
            },
          },
        ],
      },
    });
  } catch (error) {
    console.error('모달 열기 에러:', error);
    if (error.data) {
      console.error('에러 상세:', error.data);
    }
  }
};

export const handleSubmitMissionModal = async ({ ack, body, view, client }) => {
  await ack();

  try {
    const sanitizeText = (text) => {
      if (!text) return '';
      return text.replace(/\n\s*\n/g, '\n').trim();
    };

    const teamName = view.state.values[NAME_INPUT][NAME_INPUT].value;
    const subject = view.state.values[SUBJECT_INPUT][SUBJECT_INPUT].value;
    const goal = view.state.values[GOAL_INPUT][GOAL_INPUT].value;
    const rule = view.state.values[RULE_INPUT][RULE_INPUT].value;
    const plan = view.state.values[PLAN_INPUT][PLAN_INPUT].value;

    const googleSheet = new GoogleSheet();
    await googleSheet.init();

    try {
      const newRow = `${new Date().toISOString()},${sanitizeText(
        teamName
      )},${sanitizeText(subject)},${sanitizeText(goal)},${sanitizeText(
        rule
      )},${sanitizeText(plan)},,,,\n`;

      const newData = newRow.split(',');

      await googleSheet.writeMission(newData);

      // 성공 메시지 전송
      await client.chat.postMessage({
        channel: 'C0893D5CG6N',
        text: `🎯 새로운 미션이 등록되었습니다!\n*주제*: ${subject}\n*목표*: ${goal}\n*작성자*: <@${body.user.id}>`,
      });
    } catch (error) {
      // 파일 처리 실패 시 사용자에게 알림
      await client.chat.postMessage({
        channel: body.user.id,
        text: `❌ 미션 등록 중 오류가 발생했습니다: ${error.message}`,
      });
      throw error;
    }
  } catch (error) {
    console.error('에러:', JSON.stringify(error, null, 2));
  }
};
