import fs from 'fs';
import { MISSION_APPLY_MODAL } from './const.js';

export const handleApplyMission = async ({ body, client }) => {
  try {
    // CSV 파일 읽어오기
    const fileContent = fs.readFileSync('data/missions.csv', 'utf-8');
    const lines = fileContent.split('\n').filter((line) => line.trim());
    const records = lines.slice(1);

    // 미션 옵션 만들기
    const missions = records.map((record, index) => {
      const [_, __, subject, goal] = record.split(',');
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

    const fileContent = fs.readFileSync('data/missions.csv', 'utf-8');
    const lines = fileContent.split('\n').filter((line) => line.trim());

    let headers = lines[0].split(',');
    let records = lines.slice(1).map((line) => line.split(','));

    // 선택된 미션 정보 저장용
    let selectedMissions = [];

    // 1,2,3순위 처리
    const priorityMap = {
      number1: '1순위',
      number2: '2순위',
      number3: '3순위',
    };

    Object.entries(priorityMap).forEach(([priority, headerName], index) => {
      const selected = selections[priority]?.selected_option?.value;
      if (selected) {
        const missionIndex = parseInt(selected.split('_')[1]);
        const columnIndex = headers.indexOf(headerName);

        if (columnIndex !== -1) {
          // 컬럼을 찾았을 때만 처리
          let currentApplicants = records[missionIndex][columnIndex] || '';
          records[missionIndex][columnIndex] = currentApplicants
            ? `${currentApplicants};${userName}`
            : userName;
          selectedMissions.push(
            `*${index + 1}순위*: ${records[missionIndex][2]}`
          );
        }
      }
    });

    // CSV 파일로 다시 저장
    const newContent = [
      headers.join(','),
      ...records.map((record) => record.join(',')),
    ].join('\n');

    fs.writeFileSync('data/missions.csv', newContent);

    // 알림 메시지 보내기
    const messageText = [
      `🎉 ${userName}님의 미션 신청이 완료되었습니다!`,
      ...selectedMissions,
    ].join('\n');

    await client.chat.postMessage({
      channel: body.user.id,
      text: messageText,
    });
  } catch (error) {
    console.error('미션 신청 처리 중 에러:', error);
    console.error(error.stack);
    await client.chat.postMessage({
      channel: body.user.id,
      text: '미션 신청 처리 중 문제가 발생했습니다. 😢',
    });
  }
};
