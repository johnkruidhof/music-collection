/**
 * Verkleint een gekozen foto in de browser tot maximaal {@code maxSize} px (langste zijde) als JPEG.
 * {@code createImageBitmap} past de EXIF-oriëntatie toe, zodat telefoonfoto's rechtop staan; de backend
 * (ImageIO) doet dat niet. Bovendien blijft de upload klein.
 */
export async function resizeImage(file: Blob, maxSize = 1200, quality = 0.9): Promise<Blob> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    throw new Error('Dit afbeeldingsformaat wordt niet ondersteund. Kies een JPEG of PNG.');
  }
  const scale = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(bitmap.width * scale));
  canvas.height = Math.max(1, Math.round(bitmap.height * scale));
  const context = canvas.getContext('2d');
  if (!context) {
    throw new Error('Afbeelding verwerken lukt niet in deze browser.');
  }
  context.fillStyle = '#fff';
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return new Promise((resolve, reject) =>
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('Afbeelding verwerken mislukt.'))),
      'image/jpeg',
      quality,
    ),
  );
}
