import fs from 'fs';
import { MISSION_APPLY_MODAL } from './const.js';
import { GoogleSheet } from './googlesheet.js';

export const handleApplyMission = async ({ body, client }) => {
  try {
    // CSV 파일 읽어오기
    // const fileContent = fs.readFileSync('data/missions.csv', 'utf-8');
    // const lines = fileContent.split('\n').filter((line) => line.trim());
    // const records = lines.slice(1);

    const googlesheet = GoogleSheet.getInstance();
    const records = await googlesheet.readMission();

    // 미션 옵션 만들기
    const missions = records.map((record, index) => {
      const [_, __, subject, goal] = record;
      return {
        text: {
          type: 'plain_text',
          text: `${subject}`,
          emoji: true,
        },
        value: `mission_${index}`,
      };
    });

    // 모달 열기
    const result = await client.views.open({
      trigger_id: body.trigger_id,
      view: {
        type: 'modal',
        callback_id: MISSION_APPLY_MODAL,
        title: {
          type: 'plain_text',
          text: '미션 신청',
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
            type: 'actions',
            block_id: 'mission_selections',
            // optional: false,
            elements: [
              {
                type: 'static_select',
                placeholder: {
                  type: 'plain_text',
                  text: '1순위',
                },
                options: missions,
                action_id: 'number1',
              },
              {
                type: 'static_select',
                placeholder: {
                  type: 'plain_text',
                  text: '2순위',
                },
                options: missions,
                action_id: 'number2',
              },
              {
                type: 'static_select',
                placeholder: {
                  type: 'plain_text',
                  text: '3순위',
                },
                options: missions,
                action_id: 'number3',
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

export const handleApplyMissionModal = async ({ ack, body, view, client }) => {
  await ack();

  try {
    const userName = body.user.name;
    const selections = view.state.values.mission_selections;

    console.log('selections:', JSON.stringify(selections, null, 3));

    const googlesheet = GoogleSheet.getInstance();
    const records = await googlesheet.readMission();

    {
      [
        {
          fieldName: 'number1',
          rank: 1,
        },
        {
          fieldName: 'number2',
          rank: 2,
        },
        {
          fieldName: 'number3',
          rank: 3,
        },
      ].map(async item => {
        // 1순위
        // id, member, rank

        const first = selections[item.fieldName];

        if (!first.selected_option) return;

        const missionIndex = records.findIndex((record, index) => {
          const [_, __, subject] = record;

          return subject === first?.selected_option.text.text;
        });

        googlesheet.addMemberToMission(missionIndex, userName, item.rank);
      });
    }
  } catch (error) {
    console.error('미션 신청 처리 중 에러:', error);
    console.error(error.stack);
    await client.chat.postMessage({
      channel: body.user.id,
      text: '미션 신청 처리 중 문제가 발생했습니다. 😢',
    });
  }
};
