import fs from 'fs';
import { MISSION_SUBMIT_MODAL } from './const.js';

const TITLE_INPUT = 'title_input';
const SUBJECT_INPUT = 'subject_input';

export const handleSubmitMission = async ({ command, ack, client }) => {
  await ack();

  try {
    await client.views.open({
      trigger_id: command.trigger_id,
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
            block_id: 'title',
            element: {
              type: 'plain_text_input',
              action_id: TITLE_INPUT,
            },
            label: {
              type: 'plain_text',
              text: '주제를 입력하세요',
              emoji: true,
            },
          },
          {
            type: 'input',
            block_id: 'goal',
            element: {
              type: 'plain_text_input',
              action_id: SUBJECT_INPUT,
              multiline: true,
              placeholder: {
                type: 'plain_text',
                text: '목표를 적어보아요',
              },
            },
            label: {
              type: 'plain_text',
              text: '목표를 입력하세요',
              emoji: true,
            },
          },
        ],
      },
    });

    // console.log(result);
  } catch (error) {
    console.error(error);
  }
};

export const handleSubmitMissionModal = async ({ ack, body, view, client }) => {
  await ack();

  try {
    const name = body.user.name;
    const subject = view.state.values.title[TITLE_INPUT].value;
    const goal = view.state.values.goal[SUBJECT_INPUT].value;

    if (!fs.existsSync('data')) {
      fs.mkdirSync('data');
    }

    if (!fs.existsSync('data/missions.csv')) {
      fs.writeFileSync(
        'data/missions.csv',
        '날짜,이름,주제,목표,1순위,2순위,3순위,최종\n'
      );
    }

    const newRow = `${new Date().toISOString()},${name},${subject},${goal}\n`;
    fs.appendFileSync('data/missions.csv', newRow);

    await client.chat.postMessage({
      channel: 'C0893D5CG6N', // 미션방 채널 ID 찾아서 바꿔줘야 한다.
      text: `🎯 새로운 미션이 등록되었습니다!\n*주제*: ${subject}\n*목표*: ${goal}\n*작성자*: ${name}`,
    });
  } catch (error) {
    console.error('에러:', error);
  }
};
