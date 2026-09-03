export default async function handler(req: any, res: any) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');
  res.setHeader('Cache-Control', 'public, max-age=60, s-maxage=60');

  try {
    const response = await fetch('https://api.github.com/repos/Fragger7/tvmime/releases/tags/latest', {
      headers: {
        'User-Agent': 'TVMime-Version-Proxy',
        'Accept': 'application/vnd.github.v3+json'
      }
    });

    if (response.ok) {
      const release = await response.json();
      return res.status(200).json({
        versionCode: 1,
        versionName: '1.0.0',
        releaseName: release.name || 'TVMime Rolling Release',
        tvApkUrl: 'https://tvmime.vercel.app/tv.apk',
        mobileApkUrl: 'https://tvmime.vercel.app/mobile.apk',
        directTvUrl: 'https://github.com/Fragger7/tvmime/releases/download/latest/tv.apk',
        directMobileUrl: 'https://github.com/Fragger7/tvmime/releases/download/latest/mobile.apk',
        publishedAt: release.published_at,
        changelog: release.body || 'Latest continuous delivery build with low-memory streaming and playback'
      });
    }
  } catch (e) {
    // Fallback if GitHub API is unreachable
  }

  res.status(200).json({
    versionCode: 1,
    versionName: '1.0.0',
    releaseName: 'TVMime Rolling Release v1.0.0',
    tvApkUrl: 'https://tvmime.vercel.app/tv.apk',
    mobileApkUrl: 'https://tvmime.vercel.app/mobile.apk',
    directTvUrl: 'https://github.com/Fragger7/tvmime/releases/download/latest/tv.apk',
    directMobileUrl: 'https://github.com/Fragger7/tvmime/releases/download/latest/mobile.apk',
    changelog: 'TVMime standalone build with streaming catalog parser, cloud sync, and in-place OTA updater.'
  });
}
