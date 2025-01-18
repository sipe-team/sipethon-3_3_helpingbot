import fs from 'fs';
import { GoogleSheet } from './googlesheet.js';

export const handleMissionList = async ({ body, client }) => {
  try {
    const googleSheet = new GoogleSheet();
    await googleSheet.init();
    const records = await googleSheet.readMission();

    await client.views.open({
      trigger_id: body.trigger_id,
      view: {
        type: 'modal',
        title: {
          type: 'plain_text',
          text: '미션 목록',
          //   emoji: true,
        },
        blocks: [
          {
            type: 'header',
            text: {
              type: 'plain_text',
              text: '현재 진행중인 미션들',
              emoji: true,
            },
          },
          ...records.map((record, index) => {
            const [date, teamName, subject, goal, rule, plan] = record;

            return {
              type: 'section',
              text: {
                type: 'mrkdwn',
                text: `*${index + 1}. ${subject}*`,
              },
              accessory: {
                type: 'button',
                text: {
                  type: 'plain_text',
                  text: '상세 보기',
                  emoji: true,
                },
                value: `mission_${index}`,
                action_id: `view_mission_${index}`,
              },
            };
          }),
        ],
      },
    });
  } catch (error) {
    console.error('미션 목록 조회 중 에러:', error);
  }
};

export const handleViewMissionDetail = async ({ body, ack, client }) => {
  try {
    const googleSheet = new GoogleSheet();
    await googleSheet.init();
    const records = await googleSheet.readMission();

    const missionIndex = parseInt(body.actions[0].value.split('_')[1]);
    const [_, teamName, subject, goal, rule, plan] = records[missionIndex];

    // 상세 정보를 모달로 표시
    await client.views.push({
      trigger_id: body.trigger_id,
      view: {
        type: 'modal',
        title: {
          type: 'plain_text',
          text: '미션 상세 정보',
          emoji: true,
        },
        blocks: [
          {
            type: 'section',
            text: {
              type: 'mrkdwn',
              text: `*팀이름*\n${teamName ?? ''}`,
            },
          },
          {
            type: 'section',
            text: {
              type: 'mrkdwn',
              text: `*주제*\n${subject}`,
            },
          },
          {
            type: 'section',
            text: {
              type: 'mrkdwn',
              text: `*목표*\n${goal}`,
            },
          },
          {
            type: 'section',
            text: {
              type: 'mrkdwn',
              text: `*규칙*\n${rule ?? ''}`,
            },
          },
          {
            type: 'section',
            text: {
              type: 'mrkdwn',
              text: `*계획*\n${plan}`,
            },
          },
        ],
      },
    });
  } catch (error) {
    console.error('미션 상세 정보 조회 중 에러:', error);
  }
};
