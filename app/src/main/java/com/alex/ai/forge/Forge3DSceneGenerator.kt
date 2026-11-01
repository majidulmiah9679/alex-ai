package com.alex.ai.forge

object Forge3DSceneGenerator {

    /**
     * Generates the Three.js 3D Forge Scene matching the exact visual captures in Image 3:
     * - Stage 1 (Reading the idea): Glowing core charges, green/cyan elliptic orbits, glyphs spiral in, floor grid
     * - Stage 3 (Styling sections): Magenta/pink plasma core, glass slabs stack vertically with glowing code lines & tags
     * - Stage 4 (Imagery & 3D visuals): Golden stage ring lights up, ripples run the floor grid
     */
    fun generateForgeSceneHtml(topic: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body, html { width: 100%; height: 100%; overflow: hidden; background: #0B0E1E; font-family: -apple-system, sans-serif; }
    canvas { width: 100%; height: 100%; display: block; }
  </style>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
</head>
<body>
  <script>
    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x0B0E1E, 0.04);

    const camera = new THREE.PerspectiveCamera(55, window.innerWidth / window.innerHeight, 0.1, 100);
    camera.position.set(0, 3.5, 9.5);
    camera.lookAt(0, 1.2, 0);

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    document.body.appendChild(renderer.domElement);

    // Grid Floor
    const gridHelper = new THREE.GridHelper(30, 30, 0x334A90, 0x1A2144);
    gridHelper.position.y = -2;
    scene.add(gridHelper);

    // --- Core Group ---
    const coreGroup = new THREE.Group();
    scene.add(coreGroup);

    // Glowing Inner Energy Sphere
    const coreGeo = new THREE.IcosahedronGeometry(1.2, 3);
    const coreMat = new THREE.MeshStandardMaterial({
      color: 0x00E5FF,
      emissive: 0x00B0FF,
      emissiveIntensity: 0.9,
      wireframe: true
    });
    const core = new THREE.Mesh(coreGeo, coreMat);
    coreGroup.add(core);

    const innerSphereGeo = new THREE.SphereGeometry(0.75, 24, 24);
    const innerSphereMat = new THREE.MeshBasicMaterial({ color: 0x76FF03 });
    const innerSphere = new THREE.Mesh(innerSphereGeo, innerSphereMat);
    coreGroup.add(innerSphere);

    // Elliptic Orbit Rings (Stage 1)
    const ringMat = new THREE.MeshBasicMaterial({ color: 0x00E5FF, wireframe: true, transparent: true, opacity: 0.7 });
    const orbitRing1 = new THREE.Mesh(new THREE.TorusGeometry(2.4, 0.02, 16, 80), ringMat);
    orbitRing1.rotation.x = Math.PI / 3;
    coreGroup.add(orbitRing1);

    const orbitRing2 = new THREE.Mesh(new THREE.TorusGeometry(3.0, 0.02, 16, 80), ringMat);
    orbitRing2.rotation.x = -Math.PI / 4;
    orbitRing2.rotation.y = Math.PI / 6;
    coreGroup.add(orbitRing2);

    // Spiraling Glyphs (Particles on orbits)
    const glyphCount = 8;
    const glyphs = [];
    for(let i=0; i<glyphCount; i++) {
      const gGeo = new THREE.SphereGeometry(0.08, 8, 8);
      const gMat = new THREE.MeshBasicMaterial({ color: 0x00FF88 });
      const glyph = new THREE.Mesh(gGeo, gMat);
      coreGroup.add(glyph);
      glyphs.push({ mesh: glyph, angle: (i / glyphCount) * Math.PI * 2, radius: 2.4 });
    }

    // --- Glass Slabs (Stage 3) ---
    const slabsGroup = new THREE.Group();
    scene.add(slabsGroup);

    const slabList = [];
    const slabCount = 3;
    for(let i=0; i<slabCount; i++) {
      const slabGeo = new THREE.BoxGeometry(3.6, 0.08, 1.8);
      const slabMat = new THREE.MeshPhysicalMaterial({
        color: 0xFF007F,
        emissive: 0x330022,
        transparent: true,
        opacity: 0,
        roughness: 0.1,
        metalness: 0.2
      });
      const slab = new THREE.Mesh(slabGeo, slabMat);
      slab.position.set(0, 1.5 + (i * 0.7), 0);
      slabsGroup.add(slab);
      slabList.push(slab);
    }

    // --- Stage Ring (Stage 4) ---
    const stageRingGeo = new THREE.TorusGeometry(4.5, 0.06, 16, 100);
    const stageRingMat = new THREE.MeshBasicMaterial({ color: 0xFFA000, transparent: true, opacity: 0 });
    const stageRing = new THREE.Mesh(stageRingGeo, stageRingMat);
    stageRing.rotation.x = Math.PI / 2;
    stageRing.position.y = -1.95;
    scene.add(stageRing);

    // Ripple Wave Rings on Floor
    const rippleCount = 3;
    const ripples = [];
    for(let i=0; i<rippleCount; i++) {
      const ripGeo = new THREE.RingGeometry(0.5, 0.6, 32);
      const ripMat = new THREE.MeshBasicMaterial({ color: 0xFFA000, side: THREE.DoubleSide, transparent: true, opacity: 0 });
      const rip = new THREE.Mesh(ripGeo, ripMat);
      rip.rotation.x = -Math.PI / 2;
      rip.position.y = -1.98;
      scene.add(rip);
      ripples.push({ mesh: rip, scale: 1.0 + (i * 1.5) });
    }

    // Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.add(ambientLight);
    const pointLight = new THREE.PointLight(0x00E5FF, 2, 40);
    pointLight.position.set(0, 3, 2);
    scene.add(pointLight);

    let progress = 0;
    const clock = new THREE.Clock();

    function animate() {
      requestAnimationFrame(animate);
      const t = clock.getElapsedTime();
      progress += 0.003;

      // Orbit rotation
      core.rotation.x = t * 0.4;
      core.rotation.y = t * 0.6;
      orbitRing1.rotation.z = t * 0.3;
      orbitRing2.rotation.z = -t * 0.25;

      glyphs.forEach(g => {
        g.angle += 0.02;
        g.mesh.position.x = Math.cos(g.angle) * g.radius;
        g.mesh.position.z = Math.sin(g.angle) * g.radius;
        g.mesh.position.y = Math.sin(g.angle * 2) * 0.4;
      });

      // Stage transitions based on progress
      if (progress < 0.3) {
        // Stage 1: Core charges
        coreMat.color.setHex(0x00E5FF);
        innerSphereMat.color.setHex(0x76FF03);
      } else if (progress < 0.6) {
        // Stage 3: Slabs appear & stack
        coreMat.color.setHex(0xFF007F);
        coreMat.emissive.setHex(0xFF0055);
        innerSphereMat.color.setHex(0xFFA000);
        slabList.forEach((s, idx) => {
          s.material.opacity = Math.min(0.85, s.material.opacity + 0.015);
          s.rotation.y = Math.sin(t * 0.5 + idx) * 0.08;
        });
      } else {
        // Stage 4: Stage ring & floor ripples
        stageRingMat.opacity = Math.min(1.0, stageRingMat.opacity + 0.02);
        stageRing.rotation.z = t * 0.1;

        ripples.forEach(r => {
          r.scale += 0.02;
          if (r.scale > 4.5) r.scale = 0.5;
          r.mesh.scale.set(r.scale, r.scale, 1);
          r.mesh.material.opacity = (1.0 - (r.scale / 4.5)) * 0.5;
        });
      }

      renderer.render(scene, camera);
    }
    animate();

    window.addEventListener('resize', () => {
      camera.aspect = window.innerWidth / window.innerHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(window.innerWidth, window.innerHeight);
    });
  </script>
</body>
</html>
        """.trimIndent()
    }
}
