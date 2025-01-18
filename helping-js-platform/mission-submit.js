import fs from 'fs';
import { MISSION_SUBMIT_MODAL } from './const.js';
import dotenv from 'dotenv';

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
    const sanitizeForCSV = (text) => {
      if (!text) return '';
      // 값이 있을 때만 따옴표로 감싸고 이스케이프
      return text ? `"${text.replace(/\n/g, ' ').replace(/"/g, '""')}"` : '';
    };

    const teamName = view.state.values[NAME_INPUT][NAME_INPUT].value;
    const subject = view.state.values[SUBJECT_INPUT][SUBJECT_INPUT].value;
    const goal = view.state.values[GOAL_INPUT][GOAL_INPUT].value;
    const rule = view.state.values[RULE_INPUT][RULE_INPUT].value;
    const plan = view.state.values[PLAN_INPUT][PLAN_INPUT].value;

    if (!fs.existsSync('data')) {
      fs.mkdirSync('data');
    }

    try {
      // CSV 파일 처리
      let content = '';
      if (!fs.existsSync('data/missions.csv')) {
        content =
          '날짜,팀 이름,주제,목표,규칙,계획,1순위 신청자,2순위 신청자,3순위 신청자,최종 참여 인원\n';
        fs.writeFileSync('data/missions.csv', content);
      }

      const newRow = `${new Date().toISOString()},${sanitizeForCSV(
        teamName
      )},${sanitizeForCSV(subject)},${sanitizeForCSV(goal)},${sanitizeForCSV(
        rule
      )},${sanitizeForCSV(plan)},,,,\n`;
      fs.appendFileSync('data/missions.csv', newRow);

      // 성공 메시지 전송
      await client.chat.postMessage({
        channel: process.env.MISSION_CHANNER_ID,
        text: `🎯 새로운 미션이 등록되었습니다!\n*주제*: ${subject}\n*목표*: ${goal}\n*작성자*: <@${body.user.id}>`,
      });

      // // 제출한 사용자에게 DM으로 성공 메시지
      // await client.chat.postMessage({
      //   channel: body.user.id,
      //   text: `✅ 미션이 성공적으로 등록되었습니다!`,
      // });
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
