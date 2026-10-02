export const pages = [
  { slug: 'overview', title: 'Overview', group: 'Start here', description: 'Explore Composium, an in-app UI catalog and Storybook for Android Jetpack Compose.', sections: ['Why Composium', 'Feature Overview', 'Contributing', 'Summary'] },
  { slug: 'getting-started', title: 'Getting started', group: 'Start here', description: 'Install Composium from Maven Central, configure KSP, and build your first Jetpack Compose scene catalog.', sections: ['Requirements', 'Installation', 'Quick Start With KSP', 'Quick Start Without KSP'] },
  { slug: 'scenes', title: 'Scenes & catalog', group: 'Build your catalog', description: 'Organize Compose scenes, configure thumbnails and badges, and use floating tools and the SceneHost API.', sections: ['Organizing Scenes'] },
  { slug: 'parameters', title: 'Parameters & controls', group: 'Build your catalog', description: 'Explore runtime controls for Compose scene parameters, including strings, enums, sealed objects, custom options, and nullable values.', sections: ['Parameters And Controls'] },
  { slug: 'integration', title: 'App integration', group: 'Make it yours', description: 'Embed Composium in your Android app, reuse scenes in Android Studio previews, and handle wrappers and window insets.', sections: ['Android Studio Previews', 'Custom Scene Wrappers', 'Window Insets'] },
  { slug: 'theming', title: 'Theme & environment', group: 'Make it yours', description: 'Control dark theme, display size, font size, and RTL layout in your Jetpack Compose component catalog.', sections: ['Theme Control', 'Preview System Controls'] },
  { slug: 'use-cases', title: 'Use cases', group: 'Make it yours', description: 'Use Composium for Android design systems, component playgrounds, QA handoff, and local UI experimentation.', sections: ['Typical Usage Patterns'] },
];

export function splitReadme(source) {
  const sections = new Map();
  let title;
  let lines = [];
  let fence;
  for (const line of source.replace(/\r\n/g, '\n').split('\n')) {
    const marker = line.match(/^\s{0,3}(`{3,}|~{3,})/);
    if (marker) {
      if (!fence) fence = marker[1];
      else if (marker[1][0] === fence[0] && marker[1].length >= fence.length) fence = undefined;
    }
    const heading = !fence && line.match(/^## ([^\n]+)$/);
    if (heading) {
      if (title) sections.set(title, lines.join('\n'));
      title = heading[1].trim();
      lines = [];
    }
    if (title) lines.push(line);
  }
  if (title) sections.set(title, lines.join('\n'));
  return sections;
}

export function getVersion(source) {
  const match = source.match(/^Version:\s*`([^`]+)`/m);
  if (!match) throw new Error('README must declare an explicit library version.');
  return match[1];
}
