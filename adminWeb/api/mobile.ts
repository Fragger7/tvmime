export default function handler(req: any, res: any) {
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.redirect(302, 'https://github.com/Fragger7/tvmime/releases/download/latest/mobile.apk');
}
