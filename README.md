# SyncPlayer

## Introduction

This is a simple and easy to use app that enables you to control more than one device playing music through Bluetooth LE.  The architecture is a CS model, it requires one of the connecting devices to act as the server responsible to coordinate all states, and all the others to act as clients. It's originally designed to achieve a general effect that each device has the ability to manipulate the state with all commands sent to server first and then dispatched to all other clients by the server. The core part of the synchronization is to make all devices play at a same pace and it is done by eliminating noticeable latency emerging from different parts, including time consumption of sending and receiving message and the delay of `MediaPlayer` (the time `start()` is called and the time when audio card actually starts working are different) which differs from device to device. We also provide manual control to get a better effect.

Currently all songs are coded immutably and required internet to play. We do have a plan to provide more options to add extra songs from different channels. The most promising way is to play songs stored in local storage.

The basic commands include pausing, resuming, switching a song and seeking to a specific position. It should be noted that commands may be dropped now and then with no reasons, a suggested remedy is doing it twice. We will dig deep to figure out what is going on.