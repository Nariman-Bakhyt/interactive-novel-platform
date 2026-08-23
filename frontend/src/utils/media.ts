export const getMediaUrl = (path: string): string => {
  if (import.meta.env.PROD) {
    return `/media/${path}`;
  }
  return `http://127.0.0.1:9000/${path}`;
};

export const DEFAULT_COVER = getMediaUrl('interactive-novel-assets/covers/default-cover.webp');
export const DEFAULT_AVATAR = getMediaUrl('interactive-novel-assets/avatars/default-avatar.webp');
