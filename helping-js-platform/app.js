import pkg from '@slack/bolt';
const { App } = pkg;

import fs from 'fs';

import dotenv from 'dotenv';

import {
  handleSubmitMission,
  handleSubmitMissionModal,
} from './mission-submit.js';
import {
  handleApplyMission,
  handleApplyMissionModal,
} from './mission-apply.js';
import { MISSION_SUBMIT_MODAL, MISSION_APPLY_MODAL } from './const.js';

import { handleMissionList, handleViewMissionDetail } from './mission-list.js';

import * as missionFinalApi from './mission-final.js';

dotenv.config();

const app = new App({
  token: process.env.SLACK_BOT_TOKEN,
  signingSecret: process.env.SLACK_SIGNING_SECRET,
  socketMode: true,
  appToken: process.env.SLACK_APP_TOKEN,
});

app.message('ㅎㅇ', async ({ message, say }) => {
  // say() sends a message to the channel where the event was triggered
  console.log(message.user);

  await say(`Hey there <@${message.user}>!`);
});

app.command('/딱풀아', handleSubmitMission);
app.view(MISSION_SUBMIT_MODAL, handleSubmitMissionModal);

app.command('/미션신청', handleApplyMission);
app.view(MISSION_APPLY_MODAL, handleApplyMissionModal);

app.command('/미션목록', handleMissionList);
app.action(/view_mission_\d+/, handleViewMissionDetail);

app.command('/미션선발', missionFinalApi.handleMissionFinalSelect);
app.action('export_csv', missionFinalApi.handleExportCSV);
app.action('import_csv', missionFinalApi.handleImportCSV);
app.view('import_csv_modal', missionFinalApi.handleImportCSVModal);

(async () => {
  // Start your app
  await app.start(process.env.PORT || 3000);

  app.logger.info('⚡️ Bolt app is running!');
})();
