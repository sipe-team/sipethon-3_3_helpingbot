import pkg from '@slack/bolt';
const { App } = pkg;

import dotenv from 'dotenv';

import {
  handleSubmitMission,
  handleSubmitMissionModal,
} from './mission-submit.js';
import {
  handleApplyMission,
  handleApplyMissionModal,
} from './mission-apply.js';
import {
  MISSION_SUBMIT_MODAL,
  MISSION_APPLY_MODAL,
  MISSION_SUBMIT_ACTION,
  MISSION_LIST_ACTION,
  MISSION_APPLY_ACTION,
} from './const.js';

import { handleMissionList, handleViewMissionDetail } from './mission-list.js';

import * as missionFinalApi from './mission-final.js';
import { handleMission } from './mission.js';

import { GoogleSheet } from './googlesheet.js';

dotenv.config();

const app = new App({
  token: process.env.SLACK_BOT_TOKEN,
  signingSecret: process.env.SLACK_SIGNING_SECRET,
  socketMode: true,
  appToken: process.env.SLACK_APP_TOKEN,
});

app.command('/미션', handleMission);

app.action(MISSION_SUBMIT_ACTION, handleSubmitMission);
app.view(MISSION_SUBMIT_MODAL, handleSubmitMissionModal);

app.action(MISSION_LIST_ACTION, handleMissionList);
app.action(/view_mission_\d+/, handleViewMissionDetail);

app.action(MISSION_APPLY_ACTION, handleApplyMission);
app.view(MISSION_APPLY_MODAL, handleApplyMissionModal);

app.command('/미션선발', missionFinalApi.handleMissionFinalSelect);
app.action('export_csv', missionFinalApi.handleExportCSV);
app.action('import_csv', missionFinalApi.handleImportCSV);
app.view('import_csv_modal', missionFinalApi.handleImportCSVModal);

(async () => {
  // Start your app
  await app.start(process.env.PORT || 3000);

  app.logger.info('⚡️ Bolt app is running!');
})();

const googleSheet = new GoogleSheet();
await googleSheet.init();
