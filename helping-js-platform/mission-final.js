import fs from 'fs';
import dotenv from 'dotenv';

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
            block_id: 'file_actions',
            elements: [
              {
                type: 'button',
                text: {
                  type: 'plain_text',
                  text: 'Export CSV',
                  emoji: true,
                },
                style: 'primary',
                action_id: 'export_csv',
              },
              {
                type: 'button',
                text: {
                  type: 'plain_text',
                  text: 'Import CSV',
                  emoji: true,
                },
                action_id: 'import_csv',
              },
            ],
          },
          {
            type: 'divider',
          },
          //   {
          //     type: 'actions',
          //     block_id: 'mission_selections',
          //     elements: [
          //       {
          //         type: 'static_select',
          //         placeholder: {
          //           type: 'plain_text',
          //           text: '1순위',
          //         },
          //         options: missions,
          //         action_id: 'number1',
          //       },
          //       {
          //         type: 'static_select',
          //         placeholder: {
          //           type: 'plain_text',
          //           text: '2순위',
          //         },
          //         options: missions,
          //         action_id: 'number2',
          //       },
          //       {
          //         type: 'static_select',
          //         placeholder: {
          //           type: 'plain_text',
          //           text: '3순위',
          //         },
          //         options: missions,
          //         action_id: 'number3',
          //       },
          //     ],
          //   },
        ],
      },
    });
  } catch (error) {
    console.error('미션 신청 모달 열기 중 에러:', error);
  }
};

export const handleExportCSV = async ({ ack, body, client }) => {
  await ack();

  try {
    const fileContent = fs.readFileSync('data/missions.csv', 'utf-8');

    // V2 API를 사용하여 파일 업로드
    const result = await client.files.uploadV2({
      channel_id: process.env.MISSION_CHANNER_ID,
      filename: 'missions.csv',
      content: fileContent,
      initial_comment: '현재 미션 데이터입니다. 📊',
    });

    console.log('파일 업로드 성공:', result);
  } catch (error) {
    console.error('CSV 내보내기 중 에러:', error);

    // 에러 발생 시 사용자에게 알림
    await client.chat.postMessage({
      channel: process.env.MISSION_CHANNER_ID,
      text: 'CSV 파일 내보내기 중 문제가 발생했습니다. 😢',
    });
  }
};

export const handleImportCSV = async ({ ack, body, client }) => {
  await ack();

  try {
    // 새로운 모달을 스택에 추가
    await client.views.push({
      trigger_id: body.trigger_id,
      view: {
        type: 'modal',
        callback_id: 'import_csv_modal',
        title: {
          type: 'plain_text',
          text: 'CSV 파일 업로드',
          emoji: true,
        },
        blocks: [
          {
            type: 'section',
            text: {
              type: 'mrkdwn',
              text: '최종 인원이 선발된 CSV 파일을 업로드해주세요.',
            },
          },
          {
            type: 'input',
            block_id: 'file_input',
            label: {
              type: 'plain_text',
              text: 'CSV 파일 선택',
            },
            element: {
              type: 'file_input',
              action_id: 'csv_file',
            },
          },
        ],
        submit: {
          type: 'plain_text',
          text: '업로드',
          emoji: true,
        },
        private_metadata: body.view.id, // 원래 모달의 view_id 저장
      },
    });
  } catch (error) {
    console.error('파일 업로드 모달 푸시 중 에러:', error);
  }
};

export const handleImportCSVModal = async ({ ack, body, view, client }) => {
  await ack();

  try {
    const fileId = view.state.values.file_input.csv_file.files[0];

    const fileContent = fileId.preview;

    const lines = fileContent.split('\n').filter((line) => line.trim());
    const records = lines.slice(1); // 헤더 제외

    const finalSelections = records
      .map((record) => {
        const columns = record.split(',');
        return {
          subject: columns[2].trim(),
          final: columns[7] ? columns[7].trim() : '',
        };
      })
      .filter((selection) => selection.final); // 최종 컬럼에 값이 있는 것만

    const messageBlocks = [
      {
        type: 'header',
        text: {
          type: 'plain_text',
          text: '🎉 미션별 최종 선발 결과',
          emoji: true,
        },
      },
      {
        type: 'divider',
      },
      ...finalSelections.map((selection) => ({
        type: 'section',
        text: {
          type: 'mrkdwn',
          text: `*${selection.subject}*\n최종 선발: ${selection.final}`,
        },
      })),
    ];

    await client.chat.postMessage({
      channel: process.env.MISSION_CHANNER_ID,
      blocks: messageBlocks,
    });

    await client.chat.postMessage({
      channel: body.user.id,
      text: '✅ 최종 선발 결과가 채널에 공지되었습니다.',
    });
  } catch (error) {
    console.error('Error:', error);
    await client.chat.postMessage({
      channel: body.user.id,
      text: '❌ 파일 처리 중 문제가 발생했습니다: ' + error.message,
    });
  }
};
