"""
audio_generator.audio_processing
FFmpeg detection, WAV RIFF header crafting, PCM normalization, and AAC encoding.
"""

import os
import subprocess

def find_ffmpeg() -> str:
    """Finds ffmpeg executable from PATH or common winget/local paths."""
    for candidate in ["ffmpeg", "ffmpeg.exe"]:
        try:
            res = subprocess.run([candidate, "-version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if res.returncode == 0:
                return candidate
        except Exception:
            pass

    winget_path = os.path.expandvars(
        r"%LOCALAPPDATA%\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-9.0.2-full_build\bin\ffmpeg.exe"
    )
    if os.path.exists(winget_path):
        return winget_path

    local_app_data = os.environ.get("LOCALAPPDATA", "")
    if local_app_data:
        for root, _, files in os.walk(os.path.join(local_app_data, "Microsoft", "WinGet", "Packages")):
            if "ffmpeg.exe" in files:
                return os.path.join(root, "ffmpeg.exe")

    return "ffmpeg"

FFMPEG_BIN = find_ffmpeg()

def create_wav_header(
    pcm_data_len: int,
    sample_rate: int = 24000,
    channels: int = 1,
    bits_per_sample: int = 16
) -> bytes:
    """Creates a 44-byte RIFF WAV header for raw PCM bytes."""
    byte_rate = sample_rate * channels * (bits_per_sample // 8)
    block_align = channels * (bits_per_sample // 8)
    total_data_len = pcm_data_len + 36

    header = bytearray(44)
    header[0:4] = b"RIFF"
    header[4:8] = total_data_len.to_bytes(4, "little")
    header[8:12] = b"WAVE"
    header[12:16] = b"fmt "
    header[16:20] = (16).to_bytes(4, "little")
    header[20:22] = (1).to_bytes(2, "little")
    header[22:24] = channels.to_bytes(2, "little")
    header[24:28] = sample_rate.to_bytes(4, "little")
    header[28:32] = byte_rate.to_bytes(4, "little")
    header[32:34] = block_align.to_bytes(2, "little")
    header[34:36] = bits_per_sample.to_bytes(2, "little")
    header[36:40] = b"data"
    header[40:44] = pcm_data_len.to_bytes(4, "little")
    return bytes(header)

def strip_wav_header_if_present(raw_bytes: bytes) -> bytes:
    """If bytes start with RIFF header, returns only the raw PCM payload."""
    if raw_bytes.startswith(b"RIFF") and len(raw_bytes) >= 44:
        data_pos = raw_bytes.find(b"data")
        if data_pos != -1 and data_pos + 8 <= len(raw_bytes):
            return raw_bytes[data_pos + 8:]
        return raw_bytes[44:]
    return raw_bytes

def encode_pcm_to_aac(raw_audio_bytes: bytes, output_m4a_path: str):
    """
    Takes raw audio (WAV or PCM), converts and normalizes to AAC in an M4A container
    using ffmpeg loudnorm filter for studio-consistent volume.
    """
    os.makedirs(os.path.dirname(output_m4a_path), exist_ok=True)
    if raw_audio_bytes.startswith(b"RIFF"):
        wav_input = raw_audio_bytes
    else:
        wav_input = create_wav_header(len(raw_audio_bytes), sample_rate=24000) + raw_audio_bytes

    temp_output = output_m4a_path + ".tmp.m4a"
    cmd = [
        FFMPEG_BIN,
        "-y",
        "-f", "wav",
        "-i", "pipe:0",
        "-af", "loudnorm=I=-16:TP=-1.5:LRA=11",
        "-c:a", "aac",
        "-b:a", "64k",
        "-ar", "24000",
        temp_output
    ]

    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = proc.communicate(input=wav_input)
    if proc.returncode != 0:
        if os.path.exists(temp_output):
            try:
                os.remove(temp_output)
            except Exception:
                pass
        raise RuntimeError(f"FFmpeg encoding error: {stderr.decode('utf-8', errors='ignore')}")

    if os.path.exists(output_m4a_path):
        try:
            os.remove(output_m4a_path)
        except Exception:
            pass
    os.replace(temp_output, output_m4a_path)

def concatenate_and_encode_pcm_chunks(pcm_chunks: list[bytes], output_m4a_path: str):
    """
    Concatenates multiple PCM/WAV chunks seamlessly, adds a WAV header,
    and applies ffmpeg loudnorm filter to produce a normalized .m4a file.
    """
    if not pcm_chunks:
        raise ValueError("Cannot concatenate empty list of PCM chunks.")

    raw_pcm_parts = [strip_wav_header_if_present(chunk) for chunk in pcm_chunks]
    combined_pcm = b"".join(raw_pcm_parts)
    encode_pcm_to_aac(combined_pcm, output_m4a_path)
