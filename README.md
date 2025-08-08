# DeepGuard‑XRay

plugin is not actively supported

DeepGuard‑XRay is an advanced anti‑Xray plugin for Minecraft servers (Paper 1.21–1.21.4) featuring machine learning detection, staff GUI, Discord webhook integration, and command hiding functionality.

## Status

This plugin was released as an alpha version and has since been discontinued. It is provided as-is for educational use and code reference

## Features

- Machine Learning-powered mining pattern analysis to detect X‑ray cheats, distinguishing between branch mining, cave exploration, and suspicious behavior 
- Context-aware and adaptive detection with detailed reasoning reports
- Commands for in-game training and analysis:
  - `/deepguardx ml train <cheater|normal>`
  - `/deepguardx ml analyze`
  - `/deepguardx ml report`
  - `/deepguardx ml enable|disable`
  - `/deepguardx ml status` 
- Discord webhook support for real-time alert notifications
- Staff control panels including ML dashboard, training data UI, and detailed reports
- Intelligent decoy ore placement for detecting cheaters
- 6-tier custom punishment system configurable via GUI
- ParanoiaMode: hallucinations, ghost mobs, fake resources, and visual effects 
- Stealth Command Hiding added in v1.7.7: hides commands behind `deepguardx.use` permission 

## Performance

Designed to use minimal server resources—typically under 0.5% TPS impact. Supports configurable resource usage with throttled data collection 

## Commands

Use `/deepguard` or `/dgx` (alias). Key commands include:

