# Infinity Data

A program to convert the unit data from Corvus Bellis Army Builder for the Infinity tabletop game.

Currently, the main function to take an army code and to generate nice printable cards for all the units in the army.

If Corvus Belli provides an image for the unit profile it is included into the card.

The default implementation creates the cards in the size of a third of a A4 page, so 9 fit on one page for printing.

The cards look like this:

![example.png](example.png)

[kestrel army.pdf](kestrel.pdf)

## Usage

Currently, you need to execute the application in an IDE or you can download the jar, install Java 21 and executing it
with the terminal command:

``java -jar Infinity-Data-0.1-SNAPSHOT-all.jar axZrZXN0cmVsLWNvbG9uaWFsLWZvcmNlASCBLAIBAQAKAIcMAQYAAIcNAQMAAIcWAQEAAIcLAQEAAIcLAQoAAA8BCgAAhxUBAQAAhxUBAgAADgEBAACHDwEBAAIBAAUAhxABBAAAhxEBAwAAhxIBAwAAJQEBAACHFAEBAA%3D%3D``

Replace the army code with your own and program creates then an ``out\html\card`` directory with an html file with the
cards.

The html can then be opened with a browser and printed or exported into a pdf.

Optional the parameter ``false``, with a space, can be added after the army code to get the cards with cm instate of inch.

## Development

Contributions of every kind are welcome, especially if somebody has experience in HTML layout and want to help to make
the cards to look nicer.

Based on https://github.com/cwoac/Infinity-Army-Tools
