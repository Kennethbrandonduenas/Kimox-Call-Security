import React from 'react';
import renderer, {act} from 'react-test-renderer';
import {AppAccordion, AppButton, AppTextField} from '.';

describe('shared presentation components', () => {
  it('exposes button semantics and handles presses', () => {
    const onPress = jest.fn(); let tree: renderer.ReactTestRenderer;
    act(() => {tree = renderer.create(<AppButton title="Guardar" onPress={onPress}/>);});
    const button = tree!.root.findByProps({accessibilityRole: 'button'});
    act(() => button.props.onPress()); expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('reports accordion expanded state', () => {
    let tree: renderer.ReactTestRenderer;
    act(() => {tree = renderer.create(<AppAccordion header={null} expanded={false} onToggle={jest.fn()}><></></AppAccordion>);});
    expect(tree!.root.findByProps({accessibilityRole: 'button'}).props.accessibilityState).toEqual({expanded: false});
  });

  it('renders an accessible text input', () => {
    let tree: renderer.ReactTestRenderer;
    act(() => {tree = renderer.create(<AppTextField accessibilityLabel="Número" value="" onChangeText={jest.fn()}/>);});
    expect(tree!.root.findByProps({accessibilityLabel: 'Número'})).toBeTruthy();
  });
});
