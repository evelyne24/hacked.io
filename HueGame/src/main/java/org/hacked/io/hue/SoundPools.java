package org.hacked.io.hue;

/**
 * Created by evelyne24 on 21/07/2013.
 */

import android.content.Context;
import android.media.SoundPool;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SoundPools {


    private static final String TAG = "SoundPools";
    private static final int MAX_STREAMS_PER_POOL = 15;
    private List<SoundPoolContainer> containers;

    public SoundPools() {
        containers = Collections.synchronizedList(new ArrayList<SoundPoolContainer>());
    }

    public void loadSound(Context context, int rawSoundId) {
        try {
            for (SoundPoolContainer container : containers) {
                if (container.contains(rawSoundId)) {
                    return;
                }
            }
            for (SoundPoolContainer container : containers) {
                if (!container.isFull()) {
                    container.load(context, rawSoundId);
                    return;
                }
            }
            SoundPoolContainer container = new SoundPoolContainer();
            containers.add(container);
            container.load(context, rawSoundId);
        } catch (Exception e) {
            Log.w(TAG, "Load sound error", e);
        }
    }

    public void playSound(Context context, int rawSoundId) {
        try {
            for (SoundPoolContainer container : containers) {
                if (container.contains(rawSoundId)) {
                    container.play(context, rawSoundId);
                    return;
                }
            }
            for (SoundPoolContainer container : containers) {
                if (!container.isFull()) {
                    container.play(context, rawSoundId);
                    return;
                }
            }
            SoundPoolContainer container = new SoundPoolContainer();
            containers.add(container);

            container.play(context, rawSoundId);
        } catch (Exception e) {
            Log.w(TAG, "Play sound error", e);
        }
    }

    public void onPause() {
        for (SoundPoolContainer container : containers) {
            container.onPause();
        }
    }

    public void onResume() {
        for (SoundPoolContainer container : containers) {
            container.onResume();
        }
    }

    private static class SoundPoolContainer {
        SoundPool soundPool;
        Map<Integer, Integer> soundMap;
        AtomicInteger size;

        public SoundPoolContainer() {
            this.soundPool = new SoundPool(MAX_STREAMS_PER_POOL, android.media.AudioManager.STREAM_MUSIC, 0);
            this.soundMap = new ConcurrentHashMap<Integer, Integer>(MAX_STREAMS_PER_POOL);
            this.size = new AtomicInteger(0);
        }

        public void load(Context context, int rawSoundId) {
            try {
                this.size.incrementAndGet();
                soundMap.put(rawSoundId, soundPool.load(context, rawSoundId, 1));
            } catch (Exception e) {
                this.size.decrementAndGet();
                Log.w(TAG, "Load sound error", e);
            }
        }

        public void play(Context context, int rawSoundId) {
            android.media.AudioManager audioManager = (android.media.AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            final int streamVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            Integer soundId = soundMap.get(rawSoundId);
            if (soundId == null) {
                soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                        soundPool.play(sampleId, streamVolume, streamVolume, 1, 0, 1f);
                    }
                });

                this.size.incrementAndGet();
                soundPool.load(context, rawSoundId, 1);

            } else {
                try {
                    soundPool.play(soundId, streamVolume, streamVolume, 1, 0, 1f);
                } catch (Exception e) {
                    Log.w(TAG, "Play sound error", e);
                }
            }
        }

        public boolean contains(int soundId) {
            return soundMap.containsKey(soundId);
        }

        public boolean isFull() {
            return this.size.get() >= MAX_STREAMS_PER_POOL;
        }

        public void onPause() {
            try {
                soundPool.autoPause();
            } catch (Exception e) {
                Log.w(TAG, "Pause SoundPool error", e);
            }
        }

        public void onResume() {
            try {
                soundPool.autoResume();
            } catch (Exception e) {
                Log.w(TAG, "Resume SoundPool error", e);
            }
        }
    }

}