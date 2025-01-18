import {
  MISSION_APPLY_ACTION,
  MISSION_LIST_ACTION,
  MISSION_SUBMIT_ACTION,
} from './const.js';

export const handleMission = async ({ command, ack, client }) => {
  await ack();

  try {
    await client.chat.postMessage({
      channel: command.channel_id,
      blocks: [
        {
          type: 'section',
          text: {
            type: 'mrkdwn',
            text: '👋 미션 관련 메뉴입니다.',
          },
        },
        {
          type: 'actions',
          elements: [
            {
              type: 'button',
              text: {
                type: 'plain_text',
                text: '미션 발제하기',
                emoji: true,
              },
              action_id: MISSION_SUBMIT_ACTION,
            },
            {
              type: 'button',
              text: {
                type: 'plain_text',
                text: '미션 목록보기',
                emoji: true,
              },
              action_id: MISSION_LIST_ACTION,
            },
            {
              type: 'button',
              text: {
                type: 'plain_text',
                text: '미션 신청하기',
                emoji: true,
              },
              action_id: MISSION_APPLY_ACTION,
            },
          ],
        },
      ],
    });
  } catch (error) {
    console.error(error);
  }
};
