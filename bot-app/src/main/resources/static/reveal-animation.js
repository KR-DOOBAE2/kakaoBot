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
      const pose = document.createElement('span');
      pose.className = 'reveal-pose';
      character.append(pose);
      scene.append(character);
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
