import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export interface MediaTypeRule {
  extension: string;
  mime: string;
  magikaLabel?: string;
}

export interface MediaPurposeConfig {
  maxBytes: number;
  types: MediaTypeRule[];
}

export interface MediaUploadConfigFile {
  fileHeadMaxBytes: number;
  purposes: Record<string, MediaPurposeConfig>;
}

export interface LoadedMediaUploadConfig {
  fileHeadMaxBytes: number;
  purposes: Record<string, MediaPurposeConfig>;
  extensionToMimes: Readonly<Record<string, readonly string[]>>;
  magikaLabelToMime: Readonly<Record<string, string>>;
}

function resolveConfigPath(): string {
  const override = process.env.MEDIA_UPLOAD_CONFIG_PATH?.trim();
  if (override) {
    return path.resolve(override);
  }
  return path.resolve(__dirname, "../../media-upload.config.json");
}

function normalizeExtension(ext: string): string {
  const trimmed = ext.trim().toLowerCase();
  if (!trimmed.startsWith(".")) {
    throw new Error(`Extension must start with '.': ${ext}`);
  }
  return trimmed;
}

function parseConfig(raw: MediaUploadConfigFile): LoadedMediaUploadConfig {
  if (!raw.purposes || typeof raw.purposes !== "object") {
    throw new Error('media-upload.config.json: missing "purposes"');
  }

  const fileHeadMaxBytes = raw.fileHeadMaxBytes;
  if (!Number.isFinite(fileHeadMaxBytes) || fileHeadMaxBytes < 16) {
    throw new Error(
      'media-upload.config.json: "fileHeadMaxBytes" must be >= 16',
    );
  }

  const extensionToMimes: Record<string, Set<string>> = {};
  const magikaLabelToMime: Record<string, string> = {};

  for (const [purpose, purposeConfig] of Object.entries(raw.purposes)) {
    if (!purposeConfig.types?.length) {
      throw new Error(
        `media-upload.config.json: purpose "${purpose}" has no types`,
      );
    }
    if (
      !Number.isFinite(purposeConfig.maxBytes) ||
      purposeConfig.maxBytes <= 0
    ) {
      throw new Error(
        `media-upload.config.json: purpose "${purpose}" has invalid maxBytes`,
      );
    }

    for (const rule of purposeConfig.types) {
      const ext = normalizeExtension(rule.extension);
      const mime = rule.mime.trim().toLowerCase();
      if (!mime) {
        throw new Error(
          `media-upload.config.json: empty mime for ${purpose} ${ext}`,
        );
      }

      if (!extensionToMimes[ext]) {
        extensionToMimes[ext] = new Set();
      }
      extensionToMimes[ext].add(mime);

      if (rule.magikaLabel) {
        magikaLabelToMime[rule.magikaLabel.trim().toLowerCase()] = mime;
      }
    }
  }

  const frozenExtensionMap: Record<string, readonly string[]> = {};
  for (const [ext, mimes] of Object.entries(extensionToMimes)) {
    frozenExtensionMap[ext] = Object.freeze([...mimes]);
  }

  return {
    fileHeadMaxBytes,
    purposes: raw.purposes,
    extensionToMimes: Object.freeze(frozenExtensionMap),
    magikaLabelToMime: Object.freeze(magikaLabelToMime),
  };
}

function loadConfig(): LoadedMediaUploadConfig {
  const configPath = resolveConfigPath();
  let parsed: MediaUploadConfigFile;
  try {
    parsed = JSON.parse(
      readFileSync(configPath, "utf8"),
    ) as MediaUploadConfigFile;
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    throw new Error(
      `Failed to load media config from ${configPath}: ${message}`,
    );
  }
  return parseConfig(parsed);
}

export const mediaUploadConfig: LoadedMediaUploadConfig = loadConfig();
