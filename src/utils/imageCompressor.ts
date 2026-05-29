export interface CompressOptions {
  maxWidth?: number;
  maxHeight?: number;
  quality?: number; // 0.1 to 1.0
  mimeType?: string; // e.g. 'image/jpeg' or 'image/webp'
}

/**
 * Compresses an image file on the client side using HTML5 Canvas.
 * Returns the compressed File, or the original File if compression fails or results in a larger size.
 */
export function compressImage(file: File, options: CompressOptions = {}): Promise<File> {
  // If the file is not an image (or is an animated GIF, which canvas compression would break), return as is
  if (!file.type.startsWith('image/') || file.type === 'image/gif') {
    return Promise.resolve(file);
  }

  const {
    maxWidth = 1200,
    maxHeight = 1200,
    quality = 0.8,
    mimeType = 'image/jpeg'
  } = options;

  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        // Calculate new dimensions keeping the aspect ratio
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height);
          width = Math.round(width * ratio);
          height = Math.round(height * ratio);
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(file); // Fallback to original file
          return;
        }

        // Draw image onto canvas
        ctx.drawImage(img, 0, 0, width, height);

        // Convert canvas to Blob
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              resolve(file); // Fallback to original file
              return;
            }

            // Determine appropriate file extension
            let newName = file.name;
            const ext = mimeType === 'image/webp' ? '.webp' : '.jpg';
            const lastDotIndex = file.name.lastIndexOf('.');
            if (lastDotIndex !== -1) {
              newName = file.name.substring(0, lastDotIndex) + ext;
            } else {
              newName = file.name + ext;
            }

            const compressedFile = new File([blob], newName, {
              type: mimeType,
              lastModified: Date.now()
            });

            // Return compressed file only if it is smaller than original
            if (compressedFile.size < file.size) {
              resolve(compressedFile);
            } else {
              resolve(file);
            }
          },
          mimeType,
          quality
        );
      };
      img.onerror = () => {
        resolve(file); // Fallback on image loading error
      };
      img.src = event.target?.result as string;
    };
    reader.onerror = () => {
      resolve(file); // Fallback on file reading error
    };
    reader.readAsDataURL(file);
  });
}
