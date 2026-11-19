(() => {
  const cast = [
    { name: 'kurimanju', row: 5, delay: 0.05 },
    { name: 'chiikawa', row: 0, delay: 0.18 },
    { name: 'hachiware', row: 1, delay: 0.34 },
    { name: 'usagi', row: 2, delay: 0.48 },
    { name: 'momonga', row: 3, delay: 0.62 },
    { name: 'rakko', row: 4, delay: 0.82 }
  ];

  window.createCharacterRevealScene = (menu, emoji, compact = false) => {
    const scene = document.createElement('div');
    scene.className = `character-reveal${compact ? ' character-reveal-compact' : ''}`;
    scene.setAttribute('role', 'img');
    scene.setAttribute('aria-label', `여섯 친구가 각자 움직이며 ${menu} 메뉴를 공개해요`);

    for (const actor of cast) {
      const character = document.createElement('span');
      character.className = `reveal-actor reveal-${actor.name}`;
      character.style.setProperty('--row', `${actor.row * 20}%`);
      character.style.setProperty('--delay', `${actor.delay}s`);
      character.setAttribute('aria-hidden', 'true');
      const pose = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      pose.setAttribute('class', 'reveal-pose');
      pose.setAttribute('viewBox', `0 ${actor.row * 256} 256 256`);
      pose.setAttribute('aria-hidden', 'true');
      const atlas = document.createElementNS('http://www.w3.org/2000/svg', 'image');
      atlas.setAttribute('href', '/characters/reveal-sprites.png');
      atlas.setAttribute('width', '1024');
      atlas.setAttribute('height', '1536');
      pose.append(atlas);
      character.append(pose);
      scene.append(character);
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        pose.setAttribute('viewBox', `768 ${actor.row * 256} 256 256`);
      } else {
        setTimeout(() => {
          let frame = 0;
          const frames = setInterval(() => {
            frame++;
            pose.setAttribute('viewBox', `${frame * 256} ${actor.row * 256} 256 256`);
            if (frame === 3) clearInterval(frames);
          }, 350);
        }, actor.delay * 1000);
      }
    }

    const meal = document.createElement('div');
    meal.className = 'reveal-meal';
    meal.setAttribute('aria-hidden', 'true');
    const icon = document.createElement('span');
    icon.className = 'reveal-meal-icon';
    icon.textContent = emoji;
    const label = document.createElement('strong');
    label.className = 'reveal-meal-label';
    label.textContent = '두구두구…';
    meal.append(icon, label);
    scene.append(meal);
    return { scene, label };
  };
})();
