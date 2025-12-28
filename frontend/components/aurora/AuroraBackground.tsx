"use client";
import fragmentShader from "@/components/aurora/shaders/fragmentShader.glsl";
import vertexShader from "@/components/aurora/shaders/vertexShader.glsl";
import { useEffect, useRef } from "react";
import * as THREE from "three";

interface AuroraBackgroundProps {
  className?: string;
}

export default function AuroraBackground({ className = "" }: AuroraBackgroundProps) {
  // const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = document.getElementById("canvas") as HTMLCanvasElement
    if (!canvas) return
    canvasRef.current = canvas

    const scene = new THREE.Scene();
    const sizes = {
      width: window.innerWidth,
      height: window.innerHeight,
    };

    const camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);
    const renderer = new THREE.WebGLRenderer({ canvas });
    renderer.setSize(sizes.width, sizes.height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    const geometry = new THREE.PlaneGeometry(2, 2);
    const material = new THREE.ShaderMaterial({
      vertexShader,
      fragmentShader,
      uniforms: {
        uTime: { value: 0 },
        uResolution: { value: new THREE.Vector2(sizes.width, sizes.height) },
        uMouse: { value: new THREE.Vector2(0, 0) },
      },
    });

    const mesh = new THREE.Mesh(geometry, material);
    scene.add(mesh);

    const clock = new THREE.Clock();
    const render = () => {
      material.uniforms.uTime.value = clock.getElapsedTime();
      requestAnimationFrame(render);
      renderer.render(scene, camera);
    };
    render();

    const handleResize = () => {
      sizes.width = window.innerWidth;
      sizes.height = window.innerHeight;
      renderer.setSize(sizes.width, sizes.height);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      material.uniforms.uResolution.value.set(sizes.width, sizes.height);
    };

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      // renderer.dispose();
      // material.dispose();
      // geometry.dispose();
    };
  }, []);

  return (
    <>
      <canvas id="canvas" className={`fixed inset-0 -z-10 ${className}`}></canvas>
    </>
  );
}
