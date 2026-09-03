export default function handler(req: any, res: any) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');
  res.setHeader('Cache-Control', 'public, max-age=60, s-maxage=60');

  res.status(200).json({
    versionCode: 1,
    versionName: '1.0.0',
    tvApkUrl: 'https://github.com/Fragger7/tivimime/releases/download/latest/tivimime_tv_v1.0.0.apk',
    mobileApkUrl: 'https://github.com/Fragger7/tivimime/releases/download/latest/tivimime_mobile_v1.0.0.apk',
    changelog: 'Initial TiviMime standalone build with streaming catalog parser, cloud sync, and in-place OTA updater.'
  });
}
