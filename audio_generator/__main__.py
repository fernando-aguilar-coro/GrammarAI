"""
Entry point for running audio_generator package directly:
python -m audio_generator --category theory --lang en  
""" 

from .pipeline import main

if __name__ == "__main__":
    main()
