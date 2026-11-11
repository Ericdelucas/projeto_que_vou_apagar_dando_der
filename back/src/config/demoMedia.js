// Fotos ilustrativas com IDs fixos; não usar endpoints aleatórios ou páginas HTML.
// Fontes e créditos em docs/IMAGENS_DEMO.md.
export const demoImages = Object.freeze({
  steeringWheel: 'https://images.pexels.com/photos/10040636/pexels-photo-10040636.jpeg?auto=compress&cs=tinysrgb&w=960',
  headlight: 'https://images.pexels.com/photos/19318130/pexels-photo-19318130.jpeg?auto=compress&cs=tinysrgb&w=960',
  wheel: 'https://images.pexels.com/photos/8305224/pexels-photo-8305224.jpeg?auto=compress&cs=tinysrgb&w=960',
  engine: 'https://images.pexels.com/photos/190574/pexels-photo-190574.jpeg?auto=compress&cs=tinysrgb&w=960',
  car: 'https://images.pexels.com/photos/12299323/pexels-photo-12299323.jpeg?auto=compress&cs=tinysrgb&w=960',
});

export const demoPostMedia = Object.freeze([
  { imageUrl: demoImages.steeringWheel, caption: 'Inspiração para o interior do carro.' },
  { imageUrl: demoImages.headlight, caption: 'Detalhes de iluminação automotiva.' },
  { imageUrl: demoImages.wheel, caption: 'Rodas e pneus para o próximo projeto.' },
  { imageUrl: demoImages.engine, caption: 'Motor e mecânica em destaque.' },
  { imageUrl: demoImages.car, caption: 'Um projeto para quem gosta de carros.' },
]);
