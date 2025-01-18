import { GoogleSpreadsheet } from 'google-spreadsheet';
import { JWT } from 'google-auth-library';

export class GoogleSheet {
  static instance = null;

  constructor() {
    if (GoogleSheet.instance) {
      return GoogleSheet.instance;
    }

    GoogleSheet.instance = this;

    this.init();
  }

  async init() {
    const serviceAccountAuth = new JWT({
      email: process.env.GOOGLE_SERVICE_ACCOUNT_EMAIL,
      key: process.env.GOOGLE_PRIVATE_KEY.replace(/\\n/g, '\n'),
      scopes: ['https://www.googleapis.com/auth/spreadsheets'],
    });
    this.doc = new GoogleSpreadsheet(process.env.GOOGLE_SHEET_ID, serviceAccountAuth);
    await this.doc.loadInfo();
  }

  async writeMission(data) {
    try {
      const sheet = this.doc.sheetsByIndex[1]; // 미션 목록 시트
      sheet.addRow(data);
    } catch (err) {
      console.error(err);
    }
  }

  async readMission() {
    try {
      const sheet = this.doc.sheetsByIndex[1]; // 미션 목록 시트
      const rows = await sheet.getRows({ offset: 1 });
      return rows.map(({ _rawData }) => _rawData);
    } catch (err) {
      console.error(err);
    }
  }

  async addMemberToMission(missionId, member, rank) {
    try {
      const sheet = this.doc.sheetsByIndex[1];
      const rows = await sheet.getRows({ offset: 1 });
      const targetMission = rows[missionId];
      if (!targetMission) {
        throw new Error('미션을 찾을 수 없습니다.');
      }
      const rankUsers = targetMission._rawData[rank + 5];
      targetMission._rawData[rank + 5] = `${rankUsers}${rankUsers ? ',' : ''} ${member}`;
      targetMission.save();
    } catch (err) {
      console.error(err);
    }
  }

  static getInstance() {
    if (!GoogleSheet.instance) {
      GoogleSheet.instance = new GoogleSheet();
    }
    return GoogleSheet.instance;
  }
}
