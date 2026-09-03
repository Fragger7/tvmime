export default function handler(req: any, res: any) {
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.redirect(302, 'https://github.com/Fragger7/tivimime/releases/download/latest/tv.apk');
}
