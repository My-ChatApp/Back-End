import express from 'express';
import { env } from './config/env.js';
import { mediaUploadConfig } from './config/mediaUploadConfig.js';
import { checkMagikaServiceHealth } from './magika/magikaHttpClient.js';
import { errorHandler } from './middleware/errorHandler.js';
import { mediaContext } from './middleware/mediaContext.js';
import { mediaRouter } from './routes/media.routes.js';

const app = express();

app.use(express.json({ limit: '64kb' }));
app.use(mediaContext);
app.use('/api/media', mediaRouter);
app.use(errorHandler);

async function start(): Promise<void> {
  const purposes = Object.keys(mediaUploadConfig.purposes).join(', ');
  console.log(`[media-service] upload config loaded (purposes: ${purposes})`);

  console.log(`[media-service] checking magika-service at ${env.magikaServiceUrl}...`);
  await checkMagikaServiceHealth();
  console.log('[media-service] magika-service ok');

  app.listen(env.port, () => {
    console.log(`media-service listening on port ${env.port}`);
  });
}

start().catch((err) => {
  console.error('[media-service] failed to start', err);
  process.exit(1);
});
